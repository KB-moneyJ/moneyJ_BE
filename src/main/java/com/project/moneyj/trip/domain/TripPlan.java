package com.project.moneyj.trip.domain;

import com.project.moneyj.trip.dto.TripPlanPatchRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "trip_plan")
public class TripPlan {

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
    @Builder.Default
    private List<TripMember> tripMemberList = new ArrayList<>();

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