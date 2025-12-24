package com.project.moneyj.trip.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripPlanListDTO {
    private Long planId;
    private String country;
    private String countryCode;
    private String city;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer totalBudget;
    private Integer membersCount;
    private Long totalBalance;
}
