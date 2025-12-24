package com.project.moneyj.trip.domain;

import com.project.moneyj.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@Table(name = "trip_tip")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTip extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripTipId;

    private String country;

    private String tip;

    // === 생성자 (도메인 내부용) ===
    private TripTip(String country, String tip){
        this.country = country;
        this.tip = tip;
    }

    // === 정적 팩토리 메서드 ===
    public static TripTip of(String country, String tip){
        return new TripTip(country, tip);
    }
}
