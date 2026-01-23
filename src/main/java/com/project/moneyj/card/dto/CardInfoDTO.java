package com.project.moneyj.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardInfoDTO {
    private String cardName;
    private String cardNo;
    private String organization;
}
