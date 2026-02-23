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
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    // 등록된 계좌 목록 조회
    public List<CodefBankDataDTO.CodefBankAccountDTO> fetchBankAccounts(Long userId, String organization) {
        String cid = codefConnectedIdRepository.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED))
                .getConnectedId();

        BankAccountListReqDTO body = BankAccountListReqDTO.builder()
                .countryCode("KR").businessType("BK").clientType("P")
                .organization(organization)
                .connectedId(cid)
                .build();

        String url = codefProperties.getBaseUrl() + "/v1/kr/bank/p/account/account-list";
        CodefBankDataDTO data = codefApiClient.fetchAndDecode(url, body, new TypeReference<>() {});

        return data != null && data.resDepositTrust() != null
                ? data.resDepositTrust()
                : Collections.emptyList();
    }
}
