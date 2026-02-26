package com.project.moneyj.account.service.external;

import com.project.moneyj.account.dto.AccountConnectionRequestDTO;
import com.project.moneyj.account.dto.ExternalAccountDTO;

import java.util.List;

public interface AccountProvider {
    // 기관 연결
    void connectInstitution(Long userId, AccountConnectionRequestDTO request);

    // 계좌 목록 조회 명세
    List<ExternalAccountDTO> fetchBankAccounts(Long userId, String organizationCode);
}