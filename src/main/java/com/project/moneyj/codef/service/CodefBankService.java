package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.BankAccountListReqDTO;
import com.project.moneyj.codef.dto.CodefBankDataDTO;
import com.project.moneyj.codef.dto.CodefResponseDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefBankService {

    private final CodefApiClient codefApiClient;
    private final CodefProperties codefProperties;

    // 등록된 계좌 목록 조회
    public List<CodefBankDataDTO.CodefBankAccountDTO> fetchBankAccounts(String connectedId, String organization) {

        BankAccountListReqDTO req = BankAccountListReqDTO.builder()
                .countryCode("KR").businessType("BK").clientType("P")
                .organization(organization)
                .connectedId(connectedId)
                .build();

        String url = codefProperties.getBaseUrl() + "/v1/kr/bank/p/account/account-list";
        String rawResponse = codefApiClient.executePost(url, req);

        log.info("bank account-list raw={}", rawResponse);

        TypeReference<CodefResponseDTO<CodefBankDataDTO>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<CodefBankDataDTO> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

        if (responseDTO == null || !responseDTO.result().isSuccess()) {
            log.error("CODEF 계좌 조회 실패: {}", responseDTO != null ? responseDTO.result().message() : "응답 없음");
            throw MoneyjException.of(CodefErrorCode.BUSINESS_ERROR);
        }

        List<CodefBankDataDTO.CodefBankAccountDTO> depositAccounts = responseDTO.data().resDepositTrust();
        return depositAccounts != null ? depositAccounts : Collections.emptyList();
    }
}
