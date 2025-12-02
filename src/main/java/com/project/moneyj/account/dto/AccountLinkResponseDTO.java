package com.project.moneyj.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkResponseDTO {

    private Long accountId;
    private String accountName;         // 계좌명
    private String accountNumberDisplay; // 표시용 계좌번호
    private Integer balance;               // 잔액
}
