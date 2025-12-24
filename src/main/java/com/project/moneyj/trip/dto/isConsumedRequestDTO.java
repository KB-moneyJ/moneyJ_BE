package com.project.moneyj.trip.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isConsumed")
    private boolean isConsumed;
}
