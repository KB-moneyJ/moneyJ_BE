package com.project.moneyj.trip.domain;

import com.project.moneyj.common.BaseTimeEntity;
import com.project.moneyj.trip.dto.TripPlanPatchRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_plan")
public class TripPlan extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripPlanId;

    private Integer membersCount;
    private String country;
    private String countryCode;
    private String city;

    private Integer days;
    private Integer nights;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;

    private Integer totalBudget;

    private LocalDate startDate;
    private LocalDate targetDate;

    @OneToMany(mappedBy = "tripPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMember> tripMemberList = new ArrayList<>();

    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private TripPlan(Integer membersCount,
                     String country,
                     String countryCode,
                     String city,
                     Integer days,
                     Integer nights,
                     LocalDate tripStartDate,
                     LocalDate tripEndDate,
                     Integer totalBudget,
                     LocalDate startDate,
                     LocalDate targetDate) {

        this.membersCount = membersCount;
        this.country = country;
        this.countryCode = countryCode;
        this.city = city;
        this.days = days;
        this.nights = nights;
        this.tripStartDate = tripStartDate;
        this.tripEndDate = tripEndDate;
        this.totalBudget = totalBudget;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.tripMemberList = new ArrayList<>();
    }

    // === 정적 팩토리 메서드 ===
    public static TripPlan of(Integer membersCount,
                              String country,
                              String countryCode,
                              String city,
                              Integer days,
                              Integer nights,
                              LocalDate tripStartDate,
                              LocalDate tripEndDate,
                              Integer totalBudget,
                              LocalDate startDate,
                              LocalDate targetDate){

        return TripPlan.builder()
                .membersCount(membersCount)
                .country(country)
                .countryCode(countryCode)
                .city(city)
                .days(days)
                .nights(nights)
                .tripStartDate(tripStartDate)
                .tripEndDate(tripEndDate)
                .totalBudget(totalBudget)
                .startDate(startDate)
                .targetDate(targetDate)
                .build();
    }

    // Patch 비즈니스 메소드
    public void update(TripPlanPatchRequestDTO patchRequestDTO){

        if (patchRequestDTO.getCountry() != null) this.country = patchRequestDTO.getCountry();
        if (patchRequestDTO.getCountryCode() != null) this.countryCode = patchRequestDTO.getCountryCode();
        if (patchRequestDTO.getCity() != null) this.city = patchRequestDTO.getCity();
        if (patchRequestDTO.getDays() != null) this.days = patchRequestDTO.getDays();
        if (patchRequestDTO.getNights() != null) this.nights = patchRequestDTO.getNights();
        if (patchRequestDTO.getTripStartDate() != null) this.tripStartDate = patchRequestDTO.getTripStartDate();
        if (patchRequestDTO.getTripEndDate() != null) this.tripEndDate = patchRequestDTO.getTripEndDate();
        if (patchRequestDTO.getTotalBudget() != null) this.totalBudget = patchRequestDTO.getTotalBudget();
        if (patchRequestDTO.getStartDate() != null) this.startDate = patchRequestDTO.getStartDate();
        if (patchRequestDTO.getTargetDate() != null) this.targetDate = patchRequestDTO.getTargetDate();

        // 검증
        validateDates();
    }

    // 검증 메소드
    private void validateDates() {
        if (this.tripStartDate != null && this.tripEndDate != null) {
            if (this.tripStartDate.isAfter(this.tripEndDate)) {
                throw new IllegalStateException("여행 시작일은 종료일보다 늦을 수 없습니다.");
            }
        }
    }

    public void updateTotalBudget(Integer amount){

        this.totalBudget += amount;
    }
    public void updateMembersCount(Integer membersCount){
        this.membersCount = membersCount;
    }

}