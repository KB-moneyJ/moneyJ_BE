package com.project.moneyj.trip.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripBudgetRequestDTO {
    private String country;
    private String city;
    private int nights;
    private int days;
    private LocalDate startDate;
    private LocalDate endDate;
}
