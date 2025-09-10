package com.project.moneyj.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkRequestDTO {

    private String organizationCode;
    private String accountNumber;
    private Long tripPlanId;
}
