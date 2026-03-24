package com.project.moneyj.codef.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 계좌 연동 응답 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefBankDataDTO(
        List<CodefBankAccountDTO> resDepositTrust
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodefBankAccountDTO(
            String resAccount,
            String resAccountName,
            String resAccountBalance
    ) {
        public long getSafeBalance() {
            if (resAccountBalance == null || resAccountBalance.isBlank()) {
                return 0L;
            }
            try {
                return Long.parseLong(resAccountBalance);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }

}
