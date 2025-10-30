package com.project.moneyj.trip.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "trip_saving_phrase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripSavingPhrase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripSavingPhraseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_member_id")
    private TripMember tripMember;

    private String content;

    private TripSavingPhrase(TripMember tripMember, String content){
        this.tripMember = tripMember;
        this.content = content;
    }
    public static TripSavingPhrase of(TripMember tripMember, String content){
        return new TripSavingPhrase(tripMember, content);
    }

}
