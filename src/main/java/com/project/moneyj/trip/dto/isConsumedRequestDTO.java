package com.project.moneyj.trip.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class isConsumedRequestDTO {

    @NotNull
    private Long tripPlanId;

    @NotNull
    private String categoryName;

    @NotNull
    private boolean isConsumed;
}
