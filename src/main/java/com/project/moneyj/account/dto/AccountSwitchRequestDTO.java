package com.project.moneyj.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountSwitchRequestDTO {

    @NotBlank(message = "계좌번호는 필수 입력입니다.")
    @Pattern(regexp = "^[0-9]*$", message = "숫자만 입력하세요!")
    private String accountNumber;
}
