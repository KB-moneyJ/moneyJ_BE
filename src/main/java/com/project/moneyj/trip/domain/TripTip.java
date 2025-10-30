package com.project.moneyj.trip.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@Table(name = "trip_tip")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTip {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripTipId;

    private String country;

    private String tip;

    private TripTip(String country, String tip){
        this.country = country;
        this.tip = tip;
    }

    public static TripTip of(String country, String tip){
        return new TripTip(country, tip);
    }
}
