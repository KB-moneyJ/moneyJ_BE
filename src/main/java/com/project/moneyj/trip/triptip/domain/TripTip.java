package com.project.moneyj.trip.triptip.domain;

import com.project.moneyj.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
