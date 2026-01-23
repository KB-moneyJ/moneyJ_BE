package com.project.moneyj.card.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CardSwitchRequestDTO {
    private String cardName;
    private String cardNo;
}
