package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.AccountAddRequestDTO;
import com.project.moneyj.codef.dto.AccountCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.codef.util.RsaEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefCredentialService {

    private final WebClient codefWebClient;
    private final CodefProperties props;
    private final CodefAuthService authService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;
    private final CodefInstitutionRepository codefInstitutionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * CODEF에 계정(자격)을 추가하고, 그 결과를 DB에 저장/업데이트합니다.
     */
    @Transactional
    public Map<String, Object> addCredential(Long userId, AccountCreateRequestDTO.AccountInput accountInput) {
        // 1. connectedId 조회 및 비밀번호 암호화
        String connectedId = codefConnectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("사용자에 대한 ConnectedID가 없습니다."));

        if ("1".equals(accountInput.getLoginType()) && accountInput.getPassword() != null) {
            String encryptedPassword = RsaEncryptor.encryptWithPemPublicKey(accountInput.getPassword(), props.getPublicKey());
            accountInput.setPassword(encryptedPassword);
        }

        // 2. CODEF API 요청 준비
        AccountAddRequestDTO requestBody = AccountAddRequestDTO.builder()
                .connectedId(connectedId)
                .accountList(List.of(accountInput))
                .build();
        String accessToken = authService.getValidAccessToken();

        // 3. CODEF API 호출
        String rawResponse = codefWebClient.post()
                .uri(props.getBaseUrl() + "/v1/account/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 4. 공통 응답 처리 메서드를 통해 응답 파싱 및 성공 여부 검증
        Map<String, Object> responseMap = parseCodefResponse(rawResponse);

        // 5. ★★★ 성공 결과를 DB에 저장/업데이트 (Upsert) ★★★
        List<Map<String, Object>> successList = (List<Map<String, Object>>) ((Map<String, Object>) responseMap.get("data")).get("successList");

        if (successList != null && !successList.isEmpty()) {
            // 여러 기관을 동시에 추가할 수 있지만, 일반적으로 하나씩 처리하므로 첫 번째 결과를 사용
            Map<String, Object> successInfo = successList.get(0);
            saveOrUpdateInstitution(connectedId, successInfo);
        }

        log.info("CODEF 계정 추가 및 DB 상태 저장을 성공했습니다.");
        return (Map<String, Object>) responseMap.get("result");
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
                .orElseThrow(() -> new RuntimeException("커넥티드 아이디 오류"));

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
            CodefInstitution newInstitution = CodefInstitution.builder()
                    .codefConnectedId(codefConnectedId)
                    .connectedId(connectedId)
                    .organization(organization)
                    .loginType(String.valueOf(successInfo.get("loginType")))
                    .loginIdMasked(loginIdMasked)
                    .status("CONNECTED")
                    .lastVerifiedAt(LocalDateTime.now())
                    .lastResultCode((String) successInfo.get("code"))
                    .lastResultMsg((String) successInfo.get("message"))
                    .build();

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
                throw new IllegalStateException("CODEF 비즈니스 에러: " + result.get("message"));
            }
        } catch (Exception e) {
            throw new RuntimeException("CODEF 응답을 처리할 수 없습니다: " + rawResponse, e);
        }
    }

    // 2-2) 계정(자격) 목록
    @Transactional(readOnly = true)
    public Map<String, Object> listCredentials(Long userId) {
        var cid = codefConnectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Connected ID 없음")).getConnectedId();

        Map<String, Object> body = Map.of("connectedId", cid);
        String token = authService.getValidAccessToken();
        String url = props.getBaseUrl() + "/v1/account/list";

        String raw = codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("account/list raw={}", raw);

        return ApiResponseDecoder.decode(raw);
    }

    // 공용: 안전 파서
    private Map<String, Object> readAsMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            // text/plain 등 비정형 응답일 때 디버깅 도움
            throw new IllegalStateException("CODEF 응답 파싱 실패: " + raw, e);
        }
    }

    // 마스킹 메소드
    public static String maskAdvanced(String loginID) {
        if (loginID == null || loginID.length() < 4) {
            return loginID; // 너무 짧으면 마스킹하지 않음
        }
        String firstPart = loginID.substring(0, 2);
        String lastPart = loginID.substring(loginID.length() - 2);
        return firstPart + "***" + lastPart;
    }
}
