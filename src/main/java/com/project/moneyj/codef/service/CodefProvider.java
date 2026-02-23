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

    /**
     * 유저의 현재 연동 상태(Connected ID, 기관 등록 여부)를 파악하여 신규 발급 / 기관 추가 수행
     */
    @Transactional
    public void connectInstitution(Long userId, CredentialCreateRequestDTO.CredentialInput input) {
        Optional<CodefConnectedId> existingCid = connectedIdRepository.findByUserId(userId);
        if (existingCid.isEmpty()) {
            // 신규 유저 -> 새로 발급
            log.info("신규 유저입니다. Connected ID 발급 및 기관 등록을 진행합니다.");
            createConnectedId(userId, input);
        } else {
            String cid = existingCid.get().getConnectedId();
            Optional<CodefInstitution> existingInstitution = codefInstitutionRepository
                    .findByConnectedIdAndOrganization(cid, input.getOrganization());
            if (existingInstitution.isEmpty()) {
                // 커넥티드 ID는 있는데 해당 기관은 처음 -> 기관 추가 (Add)
                log.info("새로운 기관({})을 추가합니다.", input.getOrganization());
                addCredential(userId, input);
            } else {
                // 이미 등록된 기관 -> 새 비밀번호로 업데이트
                log.info("기존에 등록된 기관({})입니다. 인증 정보를 최신화합니다.", input.getOrganization());
                updateCredential(userId, input);
            }
        }
    }

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

            log.error("CODEF 계정 연동 실패 [코드: {}, 메시지: {}]", errorCode, errorMsg);

            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }

        // 정상적으로 successList가 왔을 때만 DB에 저장
        if (parsedRes != null && parsedRes.data() != null
                && parsedRes.data().successList() != null
                && !parsedRes.data().successList().isEmpty()) {
            saveOrUpdateInstitution(connectedId, parsedRes.data().successList().get(0));
        } else {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    // 은행/카드 추가
    @Transactional
    public void addCredential(Long userId, CredentialCreateRequestDTO.CredentialInput credentialInput) {
        String connectedId = connectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        var requestBody = Map.of(
                "connectedId", connectedId,
                "accountList", List.of(credentialInput)
        );

        String url = props.getBaseUrl() + "/v1/account/add";

        CodefCredentialResultDTO data = executeAccountApi(url, credentialInput, requestBody);

        saveOrUpdateInstitution(connectedId, data.successList().get(0));
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

    // CODEF의 등록된 기관의 비밀번호 등 계정 정보 업데이트
    @Transactional
    public void updateCredential(Long userId, CredentialCreateRequestDTO.CredentialInput credentialInput) {
        String connectedId = connectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        var requestBody = Map.of(
                "connectedId", connectedId,
                "accountList", List.of(credentialInput)
        );

        String url = props.getBaseUrl() + "/v1/account/update";
        CodefCredentialResultDTO data = executeAccountApi(url, credentialInput, requestBody);

        saveOrUpdateInstitution(connectedId, data.successList().get(0));
        log.info("CODEF 계정 정보 업데이트를 성공했습니다.");
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

    // ========== 헬퍼 메소드 ==========

    // add와 update에서 중복되는 암호화, API 파싱, 에러 검사를 통합한 메서드
    private CodefCredentialResultDTO executeAccountApi(String url, CredentialCreateRequestDTO.CredentialInput input, Map<String, Object> body) {

        // 비밀번호 RSA 암호화
        if ("1".equals(input.getLoginType()) && input.getPassword() != null) {
            String enc = RsaEncryptor.encryptWithPemPublicKey(input.getPassword(), props.getPublicKey());
            input.setPassword(enc);
        }

        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefCredentialResultDTO data = codefApiClient.fetchAndDecode(url, body, typeRef);

        // CODEF errorList 검사
        if (data != null && data.errorList() != null && !data.errorList().isEmpty()) {
            String errorCode = data.errorList().get(0).code();
            String errorMsg = data.errorList().get(0).message();
            log.error("CODEF 계정 연동 실패! [코드: {}, 메시지: {}]", errorCode, errorMsg);
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }

        // 성공 응답이 없으면 에러
        if (data == null || data.successList() == null || data.successList().isEmpty()) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }

        return data;
    }

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
