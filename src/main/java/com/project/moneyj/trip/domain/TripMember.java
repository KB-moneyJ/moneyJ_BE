package com.project.moneyj.trip.domain;

import com.project.moneyj.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_member")
public class TripMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id")
    private TripPlan tripPlan;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MemberRole memberRole;

    @OneToMany(mappedBy = "tripMember", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripSavingPhrase> tripSavingPhrase = new ArrayList<>();

    @OneToMany(mappedBy = "tripMember", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Category> categoryList = new ArrayList<>();

    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private TripMember(User user,
                       TripPlan tripPlan,
                       MemberRole memberRole) {

        this.user = user;
        this.tripPlan = tripPlan;
        this.memberRole = memberRole;
        this.tripSavingPhrase = new ArrayList<>();
        this.categoryList = new ArrayList<>();
    }

    // === 정적 팩토리 메서드 ===
    public static TripMember of(User user, TripPlan tripPlan, MemberRole memberRole) {

        return TripMember.builder()
                .user(user)
                .tripPlan(tripPlan)
                .memberRole(memberRole)
                .build();
    }

    // 여행 플랜 등록 연관관계 메소드
    public void enrollTripMember(User user, TripPlan tripPlan){
        this.user = user;
        user.getTripMemberList().add(this);

        this.tripPlan = tripPlan;
        tripPlan.getTripMemberList().add(this);

        // TODO 저축 플랜 문구
        // TODO 후에 방장과 멤버 역할 구분 로직 작성
        this.memberRole = MemberRole.MEMBER;
    }

    // 멤버 추가 연관관계 편의 메서드
    public void addTripMember(TripPlan tripPlan) {
        this.tripPlan = tripPlan;
        tripPlan.getTripMemberList().add(this);
    }


    public void switchIsConsumed(boolean consumed) {
        for (Category category : categoryList) {
            category.changeConsumptionStatus(consumed);
        }
    }
}
