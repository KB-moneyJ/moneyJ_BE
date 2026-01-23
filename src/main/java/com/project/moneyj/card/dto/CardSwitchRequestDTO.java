package com.project.moneyj.card.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardSwitchRequestDTO {
    private String cardName;
    @NotBlank(message = "카드번호는 필수 입력입니다.")
    private String cardNo;
    @NotBlank(message = "기관 코드는 필수 입력입니다.")
    private String organizationCode;
}
