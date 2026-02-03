package com.project.moneyj.card.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * 카드 정보 DTO
 * 카드 연결 전 사용
 */
public class CardInfoDTO {
    private String cardName;
    private String cardNo;
    private String organizationCode;
}
