package com.project.moneyj.trip.controller;


import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.transaction.service.TransactionService;
import com.project.moneyj.trip.dto.*;
import com.project.moneyj.trip.dto.TripPlanListResponseDTO;
import com.project.moneyj.trip.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.dto.TripBudgetResponseDTO;
import com.project.moneyj.trip.dto.TripBudgetRequestDTO;
import com.project.moneyj.trip.service.TripPlanService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/trip-plans")
public class TripController implements TripControllerApiSpec{

    private final TripPlanService tripPlanService;
    private final TransactionService transactionService;

    /**
     * 여행 플랜 생성
     */
    @PostMapping
    public ResponseEntity<TripPlanResponseDTO> createTripPlan(@RequestBody TripPlanRequestDTO request) {
        TripPlanResponseDTO response = tripPlanService.createTripPlans(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 여행 플랜 조회
     * 사용자별 여행 플랜 리스트 반환
     */
    @GetMapping
    public ResponseEntity<List<TripPlanListResponseDTO>> getUserTripPlans(@AuthenticationPrincipal CustomOAuth2User customUser){
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.getUserTripPlans(userId));
    }

    /**
     * 여행 플랜 상세 조회
     */
    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlanDetail(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        Long userId = customUser.getUserId();
        boolean tipExists = tripPlanService.checkSavingTip(userId, planId);
        if (tipExists) {
            tripPlanService.addSavingsTip(userId, planId);
        }
        return ResponseEntity.ok(tripPlanService.getTripPlanDetail(planId, userId));
    }

    /**
     * 여행 플랜 수정
     */
    @PatchMapping("/{planId}")
    public ResponseEntity<TripPlanResponseDTO> putPlan(
            @PathVariable Long planId,
            @RequestBody TripPlanPatchRequestDTO requestDTO){

        TripPlanResponseDTO updatedPlan = tripPlanService.patchPlan(planId, requestDTO);
        return ResponseEntity.ok(updatedPlan);
    }

    /**
     * 여행 멤버 추가
     */
    @PostMapping("/{planId}/members")
    public ResponseEntity<TripPlanResponseDTO> addTripMember(
            @PathVariable Long planId,
            @RequestBody AddTripMemberRequestDTO addTripMemberRequestDTO){

        TripPlanResponseDTO updatedPlan = tripPlanService.addTripMember(planId, addTripMemberRequestDTO);
        return ResponseEntity.ok(updatedPlan);
    }

    /**
     * 여행 플랜 탈퇴
     */
    // TODO 아무도 없는 유령 플랜 삭제 로직 추가
    @DeleteMapping("/{planId}")
    public ResponseEntity<TripPlanResponseDTO> leavePlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomOAuth2User customUser){

        Long userId = customUser.getUserId();
        TripPlanResponseDTO response = tripPlanService.leavePlan(planId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 여행 멤버별 저축 금액 및 달성률
     */
    @GetMapping("/{tripPlanId}/balances")
    public List<UserBalanceResponseDTO> getBalances(@PathVariable Long tripPlanId) {
        return tripPlanService.getUserBalances(tripPlanId);
    }

    /**
     * 여행 경비 계산
     */
    @PostMapping("/budget")
    public ResponseEntity<TripBudgetResponseDTO> getTripBudget(@RequestBody TripBudgetRequestDTO request) {
        TripBudgetResponseDTO budget = tripPlanService.getTripBudget(request);
        return ResponseEntity.ok(budget);
    }

    /**
     * 여행 플랜 카테고리별 목표 달성 여부 변경
     */
    @PostMapping("/isconsumed")
    public ResponseEntity<isConsumedResponseDTO> switchIsConsumed(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody isConsumedRequestDTO request) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.switchIsConsumed(request, userId));
    }

    /**
     * 여행 플랜 카테고리 조회
     */
    @GetMapping("/isconsumed/{planId}")
    public ResponseEntity<List<CategoryDTO>> getIsConsumed(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long planId) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.getIsConsumed(planId, userId));
    }

    /**
     * 여행 플랜 카테고리 변경
     */
    @PatchMapping("/category")
    public ResponseEntity<CategoryResponseDTO> patchCategory(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CategoryListRequestDTO request) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.patchCategory(request, userId));
    }

}
