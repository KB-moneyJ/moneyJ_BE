package com.project.moneyj.trip.plan.controller;


import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.plan.dto.category.CategoryDTO;
import com.project.moneyj.trip.plan.dto.category.CategoryListRequestDTO;
import com.project.moneyj.trip.plan.dto.category.CategoryResponseDTO;
import com.project.moneyj.trip.plan.dto.category.isConsumedRequestDTO;
import com.project.moneyj.trip.plan.dto.category.isConsumedResponseDTO;
import com.project.moneyj.trip.plan.dto.plan.TripBudgetRequestDTO;
import com.project.moneyj.trip.plan.dto.plan.TripBudgetResponseDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanDetailResponseDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanListResponseDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanRequestDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanResponseDTO;
import com.project.moneyj.trip.plan.service.TripPlanService;
import com.project.moneyj.trip.tip.service.TripTipService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/trip-plans")
public class TripPlanController implements TripPlanControllerApiSpec {

    private final TripPlanService tripPlanService;
    private final TripTipService tripTipService;

    /**
     * 여행 플랜 생성
     */
    @Override
    @PostMapping
    public ResponseEntity<TripPlanResponseDTO> createTripPlan(
        @RequestBody TripPlanRequestDTO request
    ) {
        TripPlanResponseDTO response = tripPlanService.createTripPlans(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 여행 플랜 조회
     * 사용자별 여행 플랜 리스트 반환
     */
    @Override
    @GetMapping
    public ResponseEntity<List<TripPlanListResponseDTO>> getUserTripPlans(
        @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.getUserTripPlans(userId));
    }

    /**
     * 여행 플랜 상세 조회
     */
    @Override
    @GetMapping("/{planId}")
    public ResponseEntity<TripPlanDetailResponseDTO> getPlanDetail(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ) {
        Long userId = customUser.getUserId();
        tripTipService.checkSavingTip(userId, planId);
        TripPlanDetailResponseDTO response = tripPlanService.getTripPlanDetail(planId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 여행 플랜 수정
     */
    @Override
    @PatchMapping("/{planId}")
    public ResponseEntity<TripPlanResponseDTO> putPlan(
            @PathVariable Long planId,
            @RequestBody TripPlanPatchRequestDTO requestDTO,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        TripPlanResponseDTO updatedPlan = tripPlanService.patchPlan(userId, planId, requestDTO);
        return ResponseEntity.ok(updatedPlan);
    }

    /**
     * 여행 플랜 탈퇴
     */
    @Override
    @DeleteMapping("/{planId}")
    public ResponseEntity<TripPlanResponseDTO> leavePlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        TripPlanResponseDTO response = tripPlanService.leavePlan(planId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 여행 경비 계산
     */
    // TODO : 엔드포인트, 메서드명 사전 계산임이 드러나게 바꾸기
    @Override
    @PostMapping("/budget")
    public ResponseEntity<TripBudgetResponseDTO> getTripBudget(
        @RequestBody TripBudgetRequestDTO request
    ) {
        TripBudgetResponseDTO budget = tripPlanService.getTripBudget(request);
        return ResponseEntity.ok(budget);
    }


    /**
     * 여행 플랜 카테고리별 목표 달성 여부 변경
     */
    @Override
    @PostMapping("/isconsumed")
    public ResponseEntity<isConsumedResponseDTO> switchIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody isConsumedRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.switchIsConsumed(request, userId));
    }

    /**
     * 여행 플랜 카테고리 조회
     */
    @Override
    @GetMapping("/isconsumed/{planId}")
    public ResponseEntity<List<CategoryDTO>> getIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @PathVariable Long planId
    ) {
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.getIsConsumed(planId, userId));
    }

    /**
     * 여행 플랜 카테고리 변경
     */
    @Override
    @PatchMapping("/category")
    public ResponseEntity<CategoryResponseDTO> patchCategory(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody CategoryListRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(tripPlanService.patchCategory(request, userId));
    }

}
