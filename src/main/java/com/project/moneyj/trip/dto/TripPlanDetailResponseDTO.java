package com.project.moneyj.trip.dto;

import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.domain.TripPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
public class TripPlanDetailResponseDTO {

    /**
     * 여행 플랜 상세 조회를 위한 응답 DTO
     */

    private Long planId;
    private String country;
    private String countryCode;
    private String city;

    private List<CategoryDTO> categoryDTOList;

    private Integer duration;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;

    private Integer totalBudget;
    private Integer currentSavings;

    private LocalDate startDate;
    private LocalDate targetDate;

    private List<String> savingsPhrase;
    private List<String> tripTip;

    private List<TripMemberDTO> tripMemberDTOList;

    public static TripPlanDetailResponseDTO fromEntity(
            TripPlan tripPlan,
            List<String> savingsPhrase,
            List<String> tripTip,
            List<CategoryDTO> categoryDTOList)

    {
        return TripPlanDetailResponseDTO.builder()
                .planId(tripPlan.getTripPlanId())
                .country(tripPlan.getCountry())
                .countryCode(tripPlan.getCountryCode())
                .city(tripPlan.getCity())
                .categoryDTOList(categoryDTOList)
                .duration(tripPlan.getDuration())
                .tripStartDate(tripPlan.getTripStartDate())
                .tripEndDate(tripPlan.getTripEndDate())
                .totalBudget(tripPlan.getTotalBudget())
                .currentSavings(tripPlan.getCurrentSavings())
                .startDate(tripPlan.getStartDate())
                .targetDate(tripPlan.getTargetDate())
                .savingsPhrase(savingsPhrase)
                .tripTip(tripTip)
                .tripMemberDTOList(tripPlan.getTripMemberList().stream().map(TripMemberDTO::fromEntity).toList())
                .build();
    }

}
