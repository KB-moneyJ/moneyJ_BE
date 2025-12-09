package com.project.moneyj.trip.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanRequestDTO {

    /**
     * 여행 플랜 생성 요청 DTO
     */

    @NotNull
    private String country;
    @NotNull
    private String countryCode;
    @NotNull
    private String city;

    @NotNull
    private List<CategoryDTO> categoryDTOList;

    @Min(1)
    private Integer days;

    @Min(1)
    private Integer nights;

    @NotNull
    private LocalDate tripStartDate;

    @NotNull
    private LocalDate tripEndDate;

    @NotNull @Positive
    private Integer totalBudget;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate targetDate;

    @NotNull
    private List<String> tripMemberEmail;
}
