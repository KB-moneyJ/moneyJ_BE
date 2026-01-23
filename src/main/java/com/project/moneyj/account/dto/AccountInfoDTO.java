package com.project.moneyj.account.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountInfoDTO {

    private String organizationCode;
    private String accountName;
    private String accountNumber;
    private Integer balance;
}
