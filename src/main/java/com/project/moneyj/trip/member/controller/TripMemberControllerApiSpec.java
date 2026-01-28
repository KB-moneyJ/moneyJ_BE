package com.project.moneyj.trip.member.controller;

import com.project.moneyj.trip.member.dto.AddTripMemberRequestDTO;
import com.project.moneyj.trip.member.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Trip Member", description = "여행 멤버 API")
public interface TripMemberControllerApiSpec {

    @Operation(summary = "여행 멤버 추가", description = "여행 플랜에 새 멤버를 초대/추가합니다.")
    ResponseEntity<TripPlanResponseDTO> addTripMember(
        @Parameter(description = "멤버를 추가할 플랜 ID") @PathVariable Long planId,
        @RequestBody AddTripMemberRequestDTO addTripMemberRequestDTO
    );


    @Operation(summary = "여행 멤버별 저축 금액 및 달성률 조회", description = "여행 플랜 내 멤버들의 현재 저축 금액과 달성률을 조회합니다.")
    ResponseEntity<UserBalanceResponseDTO> getBalances(
        @Parameter(description = "정산 조회할 플랜 ID") @PathVariable Long tripPlanId
    );

}

