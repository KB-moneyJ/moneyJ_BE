package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.*;
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
        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialResultDTO> parsedRes = ApiResponseDecoder.decode(rawResponseBody, typeRef);

        // 에러 처리
        if (parsedRes != null && parsedRes.data() != null && parsedRes.data().errorList() != null && !parsedRes.data().errorList().isEmpty()) {
            String errorCode = parsedRes.data().errorList().get(0).code();
            String errorMsg = parsedRes.data().errorList().get(0).message();

            log.error("CODEF 계정 연동 실패! [코드: {}, 메시지: {}]", errorCode, errorMsg);

            throw new RuntimeException("기관 연동 실패: " + errorMsg); // TODO 적절한 예외로 변경
        }

        // 정상적으로 successList가 왔을 때만 DB에 저장
        if (parsedRes != null && parsedRes.data() != null
                && parsedRes.data().successList() != null
                && !parsedRes.data().successList().isEmpty()) {
            saveOrUpdateInstitution(connectedId, parsedRes.data().successList().get(0));
        } else {
            throw new RuntimeException("기관 연동 응답을 확인할 수 없습니다."); // TODO 적절한 예외로 변경
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

        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialResultDTO> parsedRes = ApiResponseDecoder.decode(rawResponse, typeRef);

        // 에러 처리
        if (parsedRes != null && parsedRes.data() != null && parsedRes.data().errorList() != null && !parsedRes.data().errorList().isEmpty()) {
            String errorCode = parsedRes.data().errorList().get(0).code();
            String errorMsg = parsedRes.data().errorList().get(0).message();

            log.error("CODEF 계정 연동 실패! [코드: {}, 메시지: {}]", errorCode, errorMsg);

            // 에러를 무시하지 말고 무조건 던져서 흐름을 끊어야 해!
            throw new RuntimeException("기관 연동 실패: " + errorMsg); // TODO 적절한 예외로 변경
        }

        // 정상적으로 successList가 왔을 때만 DB에 저장
        if (parsedRes != null && parsedRes.data() != null
                && parsedRes.data().successList() != null
                && !parsedRes.data().successList().isEmpty()) {
            saveOrUpdateInstitution(connectedId, parsedRes.data().successList().get(0));
        } else {
            throw new RuntimeException("기관 연동 응답을 확인할 수 없습니다."); // TODO 적절한 예외로 변경
        }
        log.info("CODEF 계정 추가 및 DB 상태 저장을 성공했습니다.");
    }

    // 계정 목록 조회
    @Transactional(readOnly = true)
    public CodefCredentialListDTO listCredentials(Long userId) {
        var cid = connectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Connected ID 없음")).getConnectedId();

        Map<String, Object> body = Map.of("connectedId", cid);
        String url = props.getBaseUrl() + "/v1/account/list";

        String rawResponse = codefApiClient.executePost(url, body);

        TypeReference<CodefResponseDTO<CodefCredentialListDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialListDTO> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

        if (responseDTO == null || !responseDTO.result().isSuccess()) {
            throw MoneyjException.of(CodefErrorCode.BUSINESS_ERROR);
        }

        return responseDTO.data();
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
            TypeReference<CodefResponseDTO<Object>> typeRef = new TypeReference<>() {};
            CodefResponseDTO<Object> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

            if (responseDTO != null && responseDTO.result().isSuccess()) {
                codefInstitutionRepository.delete(institutionToDelete);
                log.info("내부 DB에서 기관({}) 정보를 삭제했습니다.", organizationCode);
            } else {
                throw MoneyjException.of(CodefErrorCode.DELETION_FAILED);
            }
        } catch (Exception e) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    // CODEF의 등록된 기관의 비밀번호 등 계정 정보 업데이트
    @Transactional
    public void updateCredential(Long userId, CredentialCreateRequestDTO.CredentialInput credentialInput) {
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

        String url = props.getBaseUrl() + "/v1/account/update";
        String rawResponse = codefApiClient.executePost(url, requestBody);

        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialResultDTO> parsedRes = ApiResponseDecoder.decode(rawResponse, typeRef);

        // 🚨 에러 리스트가 있는지 최우선으로 검사 (침묵의 에러 방지)
        if (parsedRes != null && parsedRes.data() != null && parsedRes.data().errorList() != null && !parsedRes.data().errorList().isEmpty()) {
            String errorMsg = parsedRes.data().errorList().get(0).message();
            log.error("CODEF 계정 업데이트 실패 [메시지: {}]", errorMsg);
            throw new RuntimeException("기관 연동 정보 업데이트 실패: " + errorMsg);
        }

        if (parsedRes != null && parsedRes.data() != null
                && parsedRes.data().successList() != null
                && !parsedRes.data().successList().isEmpty()) {
            saveOrUpdateInstitution(connectedId, parsedRes.data().successList().get(0));
            log.info("CODEF 계정 정보 업데이트를 성공했습니다.");
        } else {
            throw new RuntimeException("기관 연동 업데이트 응답을 확인할 수 없습니다.");
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

    private void saveOrUpdateInstitution(String connectedId, CodefCredentialResultDTO.CodefCredentialSuccessDTO successInfo) {
        String organization = successInfo.organization();

        String loginIdMasked = successInfo.id() != null ? successInfo.id() : "";
        String loginType = successInfo.loginType() != null && !successInfo.loginType().isBlank()
                ? successInfo.loginType() : "0";

        CodefConnectedId codefConnectedId = connectedIdRepository.findCodefConnectedIdByConnectedId(connectedId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        Optional<CodefInstitution> existingOpt = codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organization);

        if (existingOpt.isPresent()) {
            CodefInstitution institution = existingOpt.get();
            institution.updateConnectionStatus(
                    loginType, "CONNECTED",
                    successInfo.code(), successInfo.message(), loginIdMasked
            );
        } else {
            CodefInstitution newInstitution = CodefInstitution.of(
                    codefConnectedId, connectedId, organization,
                    loginType, loginIdMasked, "CONNECTED",
                    LocalDateTime.now(), successInfo.code(),
                    successInfo.message(), null, null
            );
            codefInstitutionRepository.save(newInstitution);
        }
    }
}
