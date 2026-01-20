package com.project.moneyj.trip.plan.dto.plan;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripBudgetResponseDTO {
    private int flightCost;
    private int accommodationCost;
    private int foodCost;
    private int totalCost;
}
