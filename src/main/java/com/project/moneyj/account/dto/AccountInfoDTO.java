package com.project.moneyj.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfoDTO {

    private String organizationCode;
    private String accountName;
    private String accountNumber;
    private Integer balance;
}
