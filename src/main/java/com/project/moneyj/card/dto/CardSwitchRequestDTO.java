package com.project.moneyj.card.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardSwitchRequestDTO {
    private String cardName;
    private String cardNo;
    private String organizationCode;
}
