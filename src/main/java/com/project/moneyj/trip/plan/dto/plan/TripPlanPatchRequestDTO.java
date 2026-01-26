package com.project.moneyj.trip.plan.dto.plan;

import com.project.moneyj.trip.plan.dto.category.CategoryDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripPlanPatchRequestDTO {

    /**
     * 여행 플랜 수정을 위한 DTO
     * 모든 필드가 null 일 수 있으므로 @NotNull 사용 X.
     */

    private String country;
    private String countryCode;
    private String city;

    private List<CategoryDTO> categoryDTOList;

    private Integer days;
    private Integer nights;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;

    private Integer totalBudget;
    private Integer currentSavings;

    private LocalDate startDate;
    private LocalDate targetDate;
}
