package com.project.moneyj.trip.dto;

import com.project.moneyj.trip.domain.TripPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class TripPlanListResponseDTO {

    /**
     * 사용자별 여행 플랜 리스트 반환을 위한 응답 DTO
     */

    private Long planId;
    private String country;
    private String countryCode;
    private String city;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer totalBudget;
    private Integer memberCount;

    public static TripPlanListResponseDTO fromEntity(TripPlan entity){
        return new TripPlanListResponseDTO(
                entity.getTripPlanId(),
                entity.getCountry(),
                entity.getCountryCode(),
                entity.getCity(),
                entity.getTripStartDate(),
                entity.getTripEndDate(),
                entity.getTotalBudget(),
                entity.getMembersCount()
        );
    }
}
