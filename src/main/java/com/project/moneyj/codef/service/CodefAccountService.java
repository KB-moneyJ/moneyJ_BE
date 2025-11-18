package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.AccountCreateRequestDTO;
import com.project.moneyj.codef.dto.AccountCreateResponseDTO;
import com.project.moneyj.codef.dto.AccountDeleteRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.codef.util.RsaEncryptor;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodefAccountService {

    private final WebClient codefWebClient;
    private final CodefProperties props;
    private final CodefAuthService codefAuthService;
    private final CodefInstitutionRepository codefInstitutionRepository;
    private final CodefConnectedIdRepository connectedIdRepository;
    private final ObjectMapper objectMapper;
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    /**
     * 최초 계정 등록 → Connected ID 발급
     * @param userId  moneyJ 사용자 ID
     * @param input   은행 로그인 정보(아이디/비번)
     */
    @Transactional
    public String createConnectedId(Long userId, AccountCreateRequestDTO.AccountInput input) {

        // 0) 이미 CID 있으면 그대로 반환
        var existing = connectedIdRepository.findByUserId(userId);
        if (existing.isPresent()) return existing.get().getConnectedId();

        // 1) 비밀번호 RSA 암호화 (loginType=1일 때)
        if ("1".equals(input.getLoginType()) && input.getPassword() != null) {
            String enc = RsaEncryptor.encryptWithPemPublicKey(input.getPassword(), props.getPublicKey());
            input.setPassword(enc);
        }

        // 2) 요청 바디 구성
        var req = AccountCreateRequestDTO.builder()
                .accountList(java.util.List.of(input))
                .build();

        // WebClient로 보내기 직전에 로그 추가
        try {
            log.info("Request Body to CODEF: {}", new ObjectMapper().writeValueAsString(req));
        } catch (Exception e) {
            log.error("Failed to serialize request body", e);
        }


        // 3) CODEF 호출
        String token = codefAuthService.getValidAccessToken();
        String url = props.getBaseUrl() + "/v1/account/create";

        // 1. 응답을 String 으로 먼저 받음.
        String rawResponseBody = codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 2. 받은 응답을 로그로 남겨서 실제 어떤 데이터가 오는지 확인.
        log.info("Raw response from CODEF: {}", rawResponseBody);

        AccountCreateResponseDTO res;

        try {
            // 3. URL 인코딩된 문자열을 디코딩.
            String decodedBody = URLDecoder.decode(rawResponseBody, StandardCharsets.UTF_8);
            log.info("Decoded response body: {}", decodedBody); // 디코딩된 JSON 문자열 확인

            // 4. 깨끗한 JSON 문자열을 객체로 변환.
            res = new ObjectMapper().readValue(decodedBody, AccountCreateResponseDTO.class);

        } catch (Exception e) {
            log.error("Failed to process CODEF response. Raw body: {}", rawResponseBody, e);
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }

        if (res == null || res.getResult() == null) {
            throw MoneyjException.of(CodefErrorCode.EMPTY_RESPONSE);
        }

        String code = res.getResult().getCode();
        String connectedId = res.getConnectedIdSafe();

        log.info("account/create result={}, txId={}, connectedId={}",
                code, res.getResult().getTransactionId(), connectedId);

        // 2-way 인증 요구(CF-03002 등)는 프론트로 그대로 전달하고 종료
        if (!"CF-00000".equals(code)) {
            // 필요 시 세부 처리 로직 추가 (2Way OTP 등)
            throw MoneyjException.of(CodefErrorCode.REGISTRATION_FAILED);
        }

        if (connectedId == null) {
            throw MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED);
        }

        // 4) DB 저장
        connectedIdRepository.save(CodefConnectedId.of(userId, connectedId, "ACTIVE"));

        Map<String, Object> responseMap = parseCodefResponse(rawResponseBody);
        List<Map<String, Object>> successList = (List<Map<String, Object>>) ((Map<String, Object>) responseMap.get("data")).get("successList");

        if (successList != null && !successList.isEmpty()) {
            // 여러 기관을 동시에 추가할 수 있지만, 일반적으로 하나씩 처리하므로 첫 번째 결과를 사용
            Map<String, Object> successInfo = successList.get(0);
            saveOrUpdateInstitution(connectedId, successInfo);
        }

        return connectedId;
    }

    /**
     * CodefInstitution 정보를 저장하거나 업데이트하는 헬퍼 메서드 (Upsert)
     */
    private void saveOrUpdateInstitution(String connectedId, Map<String, Object> successInfo) {
        String organization = (String) successInfo.get("organization");

        // CODEF 응답에서 마스킹된 아이디를 추출합니다.
        // CODEF 계정 추가 API의 성공 응답에서 마스킹된 아이디는 보통 'id'라는 키로 내려옵니다.
        String loginIdMasked = (String) successInfo.get("id");

        Optional<CodefInstitution> existingOpt = codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organization);
        CodefConnectedId codefConnectedId = codefConnectedIdRepository.findCodefConnectedIdByConnectedId(connectedId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        if (existingOpt.isPresent()) {
            // [업데이트]
            CodefInstitution institutionToUpdate = existingOpt.get();
            log.info("기존 CodefInstitution 정보를 업데이트합니다. ID: {}", institutionToUpdate.getCodefInstitutionId());

            institutionToUpdate.updateConnectionStatus(
                    String.valueOf(successInfo.get("loginType")),
                    "CONNECTED",
                    (String) successInfo.get("code"),
                    (String) successInfo.get("message"),
                    loginIdMasked // 마스킹된 아이디 전달
            );

        } else {
            // [생성]
            log.info("새로운 CodefInstitution 정보를 생성합니다.");
            CodefInstitution newInstitution = CodefInstitution.of(
                    codefConnectedId,
                    connectedId,
                    organization,
                    String.valueOf(successInfo.get("loginType")),
                    loginIdMasked,
                    "CONNECTED",
                    LocalDateTime.now(),
                    (String) successInfo.get("code"),
                    (String) successInfo.get("message"),
                    null,
                    null);

            codefInstitutionRepository.save(newInstitution);
        }
    }

    /**
     * CODEF 응답 처리를 위한 공통 헬퍼 메서드
     */
    private Map<String, Object> parseCodefResponse(String rawResponse) {
        log.info("Raw response from CODEF: {}", rawResponse);
        try {
            String decodedResponse = URLDecoder.decode(rawResponse, StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(decodedResponse, new TypeReference<>() {});
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
            String code = (String) result.get("code");

            if ("CF-00000".equals(code)) {
                return responseMap;
            } else {
                throw MoneyjException.of(CodefErrorCode.BUSINESS_ERROR);
            }
        } catch (Exception e) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    /**
     * CODEF에 연결된 계정을 삭제. (DB 연동 로직 제외)
     */
    @Transactional
    public Map<String, Object> deleteAccountFromCodef(Long userId, AccountDeleteRequestDTO request) {

        // 1~3 단계: 요청 본문 생성까지는 동일
        String connectedId = connectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND))
                .getConnectedId();

        String accessToken = codefAuthService.getValidAccessToken();

        String organizationCode = request.getOrganizationCode();
        CodefInstitution institutionToDelete = codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organizationCode)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.INSTITUTION_NOT_FOUND));

        Map<String, String> accountInfo = Map.of(
                "countryCode", "KR",
                "businessType", request.getBusinessType(),
                "clientType", "P",
                "organization", request.getOrganizationCode(),
                "loginType", request.getLoginType()
        );
        Map<String, Object> body = Map.of(
                "connectedId", connectedId,
                "accountList", List.of(accountInfo)
        );

        // 4. WebClient를 사용하여 CODEF 계정 삭제 API 호출
        String rawResponse = codefWebClient.post()
                .uri(props.getBaseUrl() + "/v1/account/delete")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(MoneyjException.of(CodefErrorCode.API_HTTP_ERROR)))
                )
                .bodyToMono(String.class)
                .block();

        // 5. 최종 응답 처리 로직
        try {
            // 5-1. 응답이 URL 인코딩 되어있을 수 있으므로 먼저 디코딩을 시도.
            String decodedResponse = URLDecoder.decode(rawResponse, StandardCharsets.UTF_8);

            // 5-2. 디코딩된 응답을 JSON(Map)으로 파싱합니다.
            Map<String, Object> responseMap = objectMapper.readValue(decodedResponse, new TypeReference<>() {});
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
            String code = (String) result.get("code");

            // 5-3. 파싱된 결과의 'code' 값으로 성공/실패를 최종 판단합니다.
            if ("CF-00000".equals(code)) {
                log.info("CODEF 계정 삭제 성공: {}", decodedResponse);
                codefInstitutionRepository.delete(institutionToDelete);
                log.info("내부 DB에서 기관({}) 정보를 삭제했습니다.", organizationCode);
                return result;
            } else {
                // 코드는 정상이지만, CODEF 비즈니스 에러인 경우
                log.error("CODEF 계정 삭제 실패: {}", decodedResponse);
                throw MoneyjException.of(CodefErrorCode.DELETION_FAILED);
            }
        } catch (Exception e) {
            // 디코딩 또는 JSON 파싱 자체에 실패하는 경우 (비표준 텍스트 응답 등)
            log.error("CODEF 응답 처리 중 심각한 오류 발생. rawResponse={}", rawResponse, e);
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }
}
