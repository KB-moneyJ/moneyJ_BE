package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.dto.CredentialCreateResponseDTO;
import com.project.moneyj.codef.dto.CredentialDeleteRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.codef.util.RsaEncryptor;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
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
public class CodefProvider {

    private final CodefProperties props;
    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;
    private final CodefInstitutionRepository codefInstitutionRepository;
    private final CodefConnectedIdRepository connectedIdRepository;
    private final ObjectMapper objectMapper;
    private final CodefApiClient codefApiClient;

    // 최초 계정 등록 → Connected ID 발급
    // 아이디 비번 입력만 해도 Connected ID 발급
    @Transactional
    public void createConnectedId(Long userId, CredentialCreateRequestDTO.CredentialInput input) {

        // 이미 CID 있으면 예외 처리
        if (connectedIdRepository.findByUserId(userId).isPresent()) {
            throw MoneyjException.of(CodefErrorCode.CONNECTED_ID_ALREADY_EXISTS);
        }

        // 비밀번호 RSA 암호화
        if ("1".equals(input.getLoginType()) && input.getPassword() != null) {
            String enc = RsaEncryptor.encryptWithPemPublicKey(input.getPassword(), props.getPublicKey());
            input.setPassword(enc);
        }

        var req = CredentialCreateRequestDTO.builder()
                .accountList(List.of(input))
                .build();

        // CODEF 호출
        String url = props.getBaseUrl() + "/v1/account/create";

        String rawResponseBody = codefApiClient.executePost(url, req);

        log.info("Raw response from CODEF: {}", rawResponseBody);

        CredentialCreateResponseDTO res = parseAndValidateCreateResponse(rawResponseBody);
        String connectedId = res.getConnectedIdSafe();

        // DB 저장
        connectedIdRepository.save(CodefConnectedId.of(userId, connectedId, "ACTIVE"));
        connectedIdRepository.flush();

        // 기관 정보 저장
        Map<String, Object> responseMap = parseCodefResponse(rawResponseBody);
        List<Map<String, Object>> successList = (List<Map<String, Object>>) ((Map<String, Object>) responseMap.get("data")).get("successList");
        if (successList != null && !successList.isEmpty()) {
            saveOrUpdateInstitution(connectedId, successList.get(0));
        }
    }

    // 은행/카드 추가
    @Transactional
    public void addCredential(Long userId, CredentialCreateRequestDTO.CredentialInput credentialInput) {
        String connectedId = connectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        if ("1".equals(credentialInput.getLoginType()) && credentialInput.getPassword() != null) {
            String encryptedPassword = RsaEncryptor.encryptWithPemPublicKey(credentialInput.getPassword(), props.getPublicKey());
            credentialInput.setPassword(encryptedPassword);
        }

        var requestBody = Map.of(
                "connectedId", connectedId,
                "accountList", List.of(credentialInput)
        );

        String url = props.getBaseUrl() + "/v1/account/add";

        String rawResponse = codefApiClient.executePost(url, requestBody);

        Map<String, Object> responseMap = parseCodefResponse(rawResponse);
        List<Map<String, Object>> successList = (List<Map<String, Object>>) ((Map<String, Object>) responseMap.get("data")).get("successList");

        if (successList != null && !successList.isEmpty()) {
            saveOrUpdateInstitution(connectedId, successList.get(0));
        }
        log.info("CODEF 계정 추가 및 DB 상태 저장을 성공했습니다.");
    }

    // 계정 목록 조회
    @Transactional(readOnly = true)
    public Map<String, Object> listCredentials(Long userId) {
        var cid = connectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Connected ID 없음")).getConnectedId();

        Map<String, Object> body = Map.of("connectedId", cid);
        String url = props.getBaseUrl() + "/v1/account/list";

        String rawResponse = codefApiClient.executePost(url, body);
        return ApiResponseDecoder.decode(rawResponse);
    }

    // CODEF 연결된 계정을 삭제
    @Transactional
    public void deleteAccountFromCodef(Long userId, CredentialDeleteRequestDTO request) {
        String connectedId = connectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND))
                .getConnectedId();

        String accessToken = codefAuthService.getValidAccessToken();
        String url = props.getBaseUrl() + "/v1/account/delete";
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

        String rawResponse = codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(MoneyjException.of(CodefErrorCode.API_HTTP_ERROR)))
                )
                .bodyToMono(String.class)
                .block();

        try {
            String decodedResponse = URLDecoder.decode(rawResponse, StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(decodedResponse, new TypeReference<>() {});
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");

            if ("CF-00000".equals(result.get("code"))) {
                codefInstitutionRepository.delete(institutionToDelete);
                log.info("내부 DB에서 기관({}) 정보를 삭제했습니다.", organizationCode);
            } else {
                throw MoneyjException.of(CodefErrorCode.DELETION_FAILED);
            }
        } catch (Exception e) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    // ========== 헬퍼 메소드 ==========

    private CredentialCreateResponseDTO parseAndValidateCreateResponse(String rawResponseBody) {
        try {
            String decodedBody = URLDecoder.decode(rawResponseBody, StandardCharsets.UTF_8);
            CredentialCreateResponseDTO res = objectMapper.readValue(decodedBody, CredentialCreateResponseDTO.class);

            if (res == null || res.getResult() == null) {
                throw MoneyjException.of(CodefErrorCode.EMPTY_RESPONSE);
            }

            String code = res.getResult().getCode();
            log.info("account/create result code={}", code);

            if (!"CF-00000".equals(code)) {
                if("CF-12803".equals(code)) throw MoneyjException.of(CodefErrorCode.INVALID_CREDENTIALS);
                throw MoneyjException.of(CodefErrorCode.REGISTRATION_FAILED);
            }

            if (res.getConnectedIdSafe() == null) {
                throw MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED);
            }
            return res;
        } catch (Exception e) {
            log.error("Failed to process CODEF create response. Raw body: {}", rawResponseBody, e);
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    private void saveOrUpdateInstitution(String connectedId, Map<String, Object> successInfo) {
        String organization = (String) successInfo.get("organization");
        String loginIdMasked = (String) successInfo.get("id");

        CodefConnectedId codefConnectedId = connectedIdRepository.findCodefConnectedIdByConnectedId(connectedId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        Optional<CodefInstitution> existingOpt = codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organization);

        if (existingOpt.isPresent()) {
            CodefInstitution institution = existingOpt.get();
            institution.updateConnectionStatus(
                    String.valueOf(successInfo.get("loginType")), "CONNECTED",
                    (String) successInfo.get("code"), (String) successInfo.get("message"), loginIdMasked
            );
        } else {
            CodefInstitution newInstitution = CodefInstitution.of(
                    codefConnectedId, connectedId, organization,
                    String.valueOf(successInfo.get("loginType")), loginIdMasked, "CONNECTED",
                    LocalDateTime.now(), (String) successInfo.get("code"),
                    (String) successInfo.get("message"), null, null
            );
            codefInstitutionRepository.save(newInstitution);
        }
    }

    private Map<String, Object> parseCodefResponse(String rawResponse) {
        try {
            String decodedResponse = URLDecoder.decode(rawResponse, StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(decodedResponse, new TypeReference<>() {});
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");

            if ("CF-00000".equals(result.get("code"))) {
                return responseMap;
            } else {
                throw MoneyjException.of(CodefErrorCode.BUSINESS_ERROR);
            }
        } catch (Exception e) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }
}
