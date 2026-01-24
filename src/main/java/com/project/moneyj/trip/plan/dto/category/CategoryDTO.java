package com.project.moneyj.trip.plan.dto.category;

import com.project.moneyj.trip.plan.domain.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryDTO {

    @NotNull
    private Long tripPlanId;

    @NotNull
    private String categoryName;

    @NotNull @PositiveOrZero
    private Integer amount;

    @NotNull
    private boolean isConsumed;


    public static CategoryDTO fromEntity(Category category, Long tripPlanId){
        return CategoryDTO.builder()
                .tripPlanId(tripPlanId)
                .categoryName(category.getCategoryName())
                .amount(category.getAmount())
                .isConsumed(category.isConsumed())
                .build();
    }
}
