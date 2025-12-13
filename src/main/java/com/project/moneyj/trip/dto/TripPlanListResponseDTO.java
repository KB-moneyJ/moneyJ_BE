package com.project.moneyj.trip.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
    private double groupProgress;

    public static TripPlanListResponseDTO of(TripPlanListDTO tripPlan, double progress){
        return new TripPlanListResponseDTO(
            tripPlan.getPlanId(),
            tripPlan.getCountry(),
            tripPlan.getCountryCode(),
            tripPlan.getCity(),
            tripPlan.getTripStartDate(),
            tripPlan.getTripEndDate(),
            tripPlan.getTotalBudget(),
            tripPlan.getMembersCount(),
            progress
        );
    }
}
