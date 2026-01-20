package com.project.moneyj.codef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.BankAccountListReqDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefBankService {

    private final WebClient codefWebClient;
    private final CodefProperties codefProperties;
    private final CodefAuthService codefAuthService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    // 등록된 계좌 목록 조회
    public Map<String, Object> fetchBankAccounts(Long userId, String organization) {
        String cid = codefConnectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED))
                .getConnectedId();

        BankAccountListReqDTO req = BankAccountListReqDTO.builder()
                .countryCode("KR").businessType("BK").clientType("P")
                .organization(organization)
                .connectedId(cid)
                .build();

        String token = codefAuthService.getValidAccessToken();
        // 예시 경로 (필요 시 문서 기준으로 조정)
        String url = codefProperties.getBaseUrl() + "/v1/kr/bank/p/account/account-list";

        String raw = codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("bank account-list raw={}", raw);

        return ApiResponseDecoder.decode(raw);
    }
}
