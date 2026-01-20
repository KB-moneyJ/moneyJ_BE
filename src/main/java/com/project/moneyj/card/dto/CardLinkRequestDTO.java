package com.project.moneyj.card.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CardLinkRequestDTO {
    private String cardName;
    private String cardNo;
    private String organization;
}