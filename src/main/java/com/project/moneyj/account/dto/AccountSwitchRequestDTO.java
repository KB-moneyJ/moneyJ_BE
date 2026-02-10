package com.project.moneyj.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 계좌 변경 요청 DTO
 */
public class AccountSwitchRequestDTO {

    @NotBlank(message = "계좌번호는 필수 입력입니다.")
    @Pattern(regexp = "^[0-9]*$", message = "숫자만 입력하세요!")
    private String accountNumber;

    @NotNull(message = "잔액은 필수 입력입니다.")
    private Integer balance;

    @NotBlank(message = "기관 코드는 필수 입력입니다.")
    private String organizationCode;

    private String accountName;
}
