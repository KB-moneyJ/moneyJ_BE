package com.project.moneyj.trip.tip.domain;

import com.project.moneyj.common.BaseTimeEntity;
import com.project.moneyj.trip.member.domain.TripMember;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "trip_saving_phrase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripSavingPhrase extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripSavingPhraseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_member_id")
    private TripMember tripMember;

    private String content;

    // === 생성자 (도메인 내부용) ===
    private TripSavingPhrase(TripMember tripMember, String content){
        this.tripMember = tripMember;
        this.content = content;
    }

    // === 정적 팩토리 메서드 ===
    public static TripSavingPhrase of(TripMember tripMember, String content){
        return new TripSavingPhrase(tripMember, content);
    }

}
