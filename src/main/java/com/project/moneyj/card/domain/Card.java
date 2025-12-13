package com.project.moneyj.card.domain;

import com.project.moneyj.common.BaseTimeEntity;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "card")
public class Card extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String cardNo;

    private String cardPassword;

    private String organizationCode;

    private String cardName;

    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private Card(User user,
                 String cardNo,
                 String cardPassword,
                 String organizationCode,
                 String cardName) {

        this.user = user;
        this.cardNo = cardNo;
        this.cardPassword = cardPassword;
        this.organizationCode = organizationCode;
        this.cardName = cardName;
    }

    // === 정적 팩토리 메서드 ===
    public static Card of(User user,
                          String cardNo,
                          String cardPassword,
                          String organizationCode,
                          String cardName) {

        return Card.builder()
                .user(user)
                .cardNo(cardNo)
                .cardPassword(cardPassword)
                .organizationCode(organizationCode)
                .cardName(cardName)
                .build();
    }

    // ==== 비즈니스 메서드 ====
    public void setCardName(String cardName){
        this.cardName = cardName;
    }
}

