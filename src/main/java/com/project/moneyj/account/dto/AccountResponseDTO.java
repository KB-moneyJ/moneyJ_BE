package com.project.moneyj.account.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountResponseDTO {

    private Long accountId;
    private String accountName;         // 계좌명
    private String accountNumber; // 표시용 계좌번호
    private Integer balance;               // 잔액
}
