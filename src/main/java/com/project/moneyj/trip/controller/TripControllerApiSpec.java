package com.project.moneyj.trip.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Trip Plans", description = "여행 플랜 API")
public interface TripControllerApiSpec {

    @Operation(summary = "여행 플랜 생성", description = "새로운 여행 플랜을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "플랜 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    ResponseEntity<TripPlanResponseDTO> createTripPlan(@RequestBody TripPlanRequestDTO request);

    @Operation(summary = "여행 플랜 리스트 조회", description = "로그인한 사용자의 모든 여행 플랜 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    ResponseEntity<List<TripPlanListResponseDTO>> getUserTripPlans(

            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User customUser);

    @Operation(summary = "여행 플랜 상세 조회", description = "특정 여행 플랜의 상세 내용을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "플랜을 찾을 수 없음")
    })
    @GetMapping("/{planId}")
    ResponseEntity<?> getPlanDetail(
            @Parameter(description = "조회할 플랜 ID") @PathVariable Long planId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User customUser);

    @Operation(summary = "여행 플랜 수정", description = "특정 여행 플랜의 내용을 수정합니다.")
    @PatchMapping("/{planId}")
    ResponseEntity<TripPlanResponseDTO> putPlan(
            @Parameter(description = "수정할 플랜 ID") @PathVariable Long planId,
            @RequestBody TripPlanPatchRequestDTO requestDTO
    );

    @Operation(summary = "여행 멤버 추가", description = "여행 플랜에 새 멤버를 초대/추가합니다.")
    @PostMapping("/{planId}/members")
    ResponseEntity<TripPlanResponseDTO> addTripMember(
            @Parameter(description = "멤버를 추가할 플랜 ID") @PathVariable Long planId,
            @RequestBody AddTripMemberRequestDTO addTripMemberRequestDTO
    );

    @Operation(summary = "여행 플랜 탈퇴", description = "참여 중인 여행 플랜에서 나갑니다.")
    @DeleteMapping("/{planId}")
    ResponseEntity<TripPlanResponseDTO> leavePlan(
            @Parameter(description = "탈퇴할 플랜 ID") @PathVariable Long planId,
            @AuthenticationPrincipal CustomOAuth2User customUser
    );

    @Operation(summary = "여행 멤버별 저축 금액 및 달성률 조회", description = "여행 플랜 내 멤버들의 현재 저축 금액과 달성률을 조회합니다.")
    @GetMapping("/{tripPlanId}/balances")
    List<UserBalanceResponseDTO> getBalances(
            @Parameter(description = "정산 조회할 플랜 ID") @PathVariable Long tripPlanId
    );

    @Operation(summary = "여행 경비 계산", description = "여행에 필요한 예상 경비를 계산합니다.")
    @PostMapping("/budget")
    ResponseEntity<TripBudgetResponseDTO> getTripBudget(
            @RequestBody TripBudgetRequestDTO request
    );

    @Operation(summary = "카테고리 목표 달성 여부 변경", description = "특정 카테고리의 소비 완료 여부를 토글합니다.")
    @PostMapping("/isconsumed")
    ResponseEntity<isConsumedResponseDTO> switchIsConsumed(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody isConsumedRequestDTO request
    );

    @Operation(summary = "카테고리별 달성 여부 조회", description = "플랜의 모든 카테고리별 소비 완료 여부를 조회합니다.")
    @GetMapping("/isconsumed/{planId}")
    ResponseEntity<List<CategoryDTO>> getIsConsumed(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @Parameter(description = "조회할 플랜 ID") @PathVariable Long planId
    );

    @Operation(summary = "카테고리 변경", description = "여행 플랜의 카테고리 목록을 수정합니다.")
    @PatchMapping("/category")
    ResponseEntity<CategoryResponseDTO> patchCategory(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CategoryListRequestDTO request
    );

}

