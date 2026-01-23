package com.project.moneyj.card.dto;

import com.project.moneyj.card.domain.Card;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardResponseDTO {
    private Long cardId;
    private String cardName;
    private String cardNo;
    private String organization;

    public static CardResponseDTO from(Card card) {
        return CardResponseDTO.builder()
                .cardId(card.getCardId())
                .cardName(card.getCardName())
                .cardNo(card.getCardNo())
                .organization(card.getOrganization())
                .build();
    }
}
