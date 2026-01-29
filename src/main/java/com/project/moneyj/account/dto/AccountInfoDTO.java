package com.project.moneyj.account.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * 계좌 정보 DTO
 * 계좌 연결 전 사용
 */
public class AccountInfoDTO {

    private String organizationCode;
    private String accountName;
    private String accountNumber;
    private Integer balance;
}
