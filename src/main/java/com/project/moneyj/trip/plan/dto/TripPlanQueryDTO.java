package com.project.moneyj.trip.plan.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripPlanQueryDTO {
    private Long planId;
    private String country;
    private String countryCode;
    private String city;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer totalBudget;
    private Integer membersCount;
    private Long totalBalance;


    @QueryProjection
    public TripPlanQueryDTO(
        Long planId,
        String country,
        String countryCode,
        String city,
        LocalDate tripStartDate,
        LocalDate tripEndDate,
        Integer totalBudget,
        Integer membersCount,
        Long accountBalance,
        Long categoryBalance
    ) {
        this.planId = planId;
        this.country = country;
        this.countryCode = countryCode;
        this.city = city;
        this.tripStartDate = tripStartDate;
        this.tripEndDate = tripEndDate;
        this.totalBudget = totalBudget;
        this.membersCount = membersCount;
        this.totalBalance = accountBalance + categoryBalance;
    }

}
