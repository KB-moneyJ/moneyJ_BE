package com.project.moneyj.trip.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TripPlanResponseDTO {

    /**
     * 여행 생성, 수정, 삭제 응답 DTO
     */

    private Long planId;
    private String message;
}
