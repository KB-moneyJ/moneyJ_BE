package com.project.moneyj.trip.member.dto.category;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class isConsumedResponseDTO {

    private String message;

    @NotNull
    private boolean isConsumed;
}
