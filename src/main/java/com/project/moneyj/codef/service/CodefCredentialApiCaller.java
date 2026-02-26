package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.*;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodefCredentialApiCaller {

    private final CodefApiClient codefApiClient;
    private final CodefProperties props;
    private final ObjectMapper objectMapper;
    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;

    public record CreateCredentialResult(String connectedId, CodefCredentialResultDTO.CodefCredentialSuccessDTO successInfo) {}

    // 최초 계정 등록 통신
    public CreateCredentialResult createAccount(CredentialCreateRequestDTO.CredentialInput input) {
        encryptPasswordIfNeed(input);
        var req = CredentialCreateRequestDTO.builder().accountList(List.of(input)).build();
        String url = props.getBaseUrl() + "/v1/account/create";

        String rawResponseBody = codefApiClient.executePost(url, req);
        log.info("Raw response from CODEF: {}", rawResponseBody);

        // Connected ID 파싱 (기존 로직)
        CredentialCreateResponseDTO res = parseAndValidateCreateResponse(rawResponseBody);
        String connectedId = res.getConnectedIdSafe();

        // 기관 정보 파싱 (기존 로직)
        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialResultDTO> parsedRes = ApiResponseDecoder.decode(rawResponseBody, typeRef);

        validateResult(parsedRes);
        return new CreateCredentialResult(connectedId, parsedRes.data().successList().get(0));
    }

    // 기관 추가 / 기관 업데이트 통신
    public CodefCredentialResultDTO.CodefCredentialSuccessDTO addOrUpdateAccount(String urlPath, String connectedId, CredentialCreateRequestDTO.CredentialInput input) {
        encryptPasswordIfNeed(input);
        var requestBody = Map.of("connectedId", connectedId, "accountList", List.of(input));
        String url = props.getBaseUrl() + urlPath;

        String rawResponse = codefApiClient.executePost(url, requestBody);

        TypeReference<CodefResponseDTO<CodefCredentialResultDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialResultDTO> parsedRes = ApiResponseDecoder.decode(rawResponse, typeRef);

        validateResult(parsedRes);
        return parsedRes.data().successList().get(0);
    }

    // 계정 목록 조회 통신
    public CodefCredentialListDTO listCredentials(String connectedId) {
        Map<String, Object> body = Map.of("connectedId", connectedId);
        String url = props.getBaseUrl() + "/v1/account/list";

        String rawResponse = codefApiClient.executePost(url, body);

        TypeReference<CodefResponseDTO<CodefCredentialListDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefCredentialListDTO> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

        if (responseDTO == null || !responseDTO.result().isSuccess()) {
            throw MoneyjException.of(CodefErrorCode.BUSINESS_ERROR);
        }
        return responseDTO.data();
    }

    // 계정 삭제 통신 (기존 WebClient 로직 그대로)
    public void deleteAccountFromCodef(String connectedId, CredentialDeleteRequestDTO request) {
        String accessToken = codefAuthService.getValidAccessToken();
        String url = props.getBaseUrl() + "/v1/account/delete";

        Map<String, String> accountInfo = Map.of(
                "countryCode", "KR", "businessType", request.getBusinessType(),
                "clientType", "P", "organization", request.getOrganizationCode(), "loginType", request.getLoginType()
        );
        Map<String, Object> body = Map.of("connectedId", connectedId, "accountList", List.of(accountInfo));

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
            if (responseDTO == null || !responseDTO.result().isSuccess()) {
                throw MoneyjException.of(CodefErrorCode.DELETION_FAILED);
            }
        } catch (Exception e) {
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    // --- 헬퍼 메서드 ---
    private void encryptPasswordIfNeed(CredentialCreateRequestDTO.CredentialInput input) {
        if ("1".equals(input.getLoginType()) && input.getPassword() != null) {
            String enc = RsaEncryptor.encryptWithPemPublicKey(input.getPassword(), props.getPublicKey());
            input.setPassword(enc);
        }
    }

    private CredentialCreateResponseDTO parseAndValidateCreateResponse(String rawResponseBody) {
        try {
            String decodedBody = URLDecoder.decode(rawResponseBody, StandardCharsets.UTF_8);
            CredentialCreateResponseDTO res = objectMapper.readValue(decodedBody, CredentialCreateResponseDTO.class);

            if (res == null || res.getResult() == null) throw MoneyjException.of(CodefErrorCode.EMPTY_RESPONSE);

            String code = res.getResult().getCode();
            log.info("account/create result code={}", code);

            if (!"CF-00000".equals(code)) {
                if("CF-12803".equals(code)) throw MoneyjException.of(CodefErrorCode.INVALID_CREDENTIALS);
                throw MoneyjException.of(CodefErrorCode.REGISTRATION_FAILED);
            }
            if (res.getConnectedIdSafe() == null) throw MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED);
            return res;
        } catch (Exception e) {
            log.error("Failed to process CODEF create response.", e);
            throw MoneyjException.of(CodefErrorCode.RESPONSE_PARSE_FAILED);
        }
    }

    private void validateResult(CodefResponseDTO<CodefCredentialResultDTO> parsedRes) {
        if (parsedRes != null && parsedRes.data() != null && parsedRes.data().errorList() != null && !parsedRes.data().errorList().isEmpty()) {
            String errorCode = parsedRes.data().errorList().get(0).code();
            String errorMsg = parsedRes.data().errorList().get(0).message();
            log.error("CODEF 계정 연동/업데이트 실패! [코드: {}, 메시지: {}]", errorCode, errorMsg);
            throw new RuntimeException("기관 연동 실패: " + errorMsg);
        }
        if (parsedRes == null || parsedRes.data() == null || parsedRes.data().successList() == null || parsedRes.data().successList().isEmpty()) {
            throw new RuntimeException("기관 연동 응답을 확인할 수 없습니다.");
        }
    }
}