package com.project.moneyj.trip.plan.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryResponseDTO {

    private String message;

    private String categoryName;

    private Integer amount;

}
