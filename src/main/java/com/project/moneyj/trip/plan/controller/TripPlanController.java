package com.project.moneyj.trip.plan.controller;


import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.plan.dto.TripBudgetRequestDTO;
import com.project.moneyj.trip.plan.dto.TripBudgetResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanDetailResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanListResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.plan.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.plan.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.plan.service.TripPlanFacade;
import com.project.moneyj.trip.plan.service.TripPlanService;
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

    private final TripPlanFacade tripPlanFacade;
    private final TripPlanService tripPlanService;

    /**
     * 여행 플랜 생성
     */
    @Override
    @PostMapping
    public ResponseEntity<TripPlanResponseDTO> createTripPlan(
        @RequestBody TripPlanRequestDTO request
    ) {
        TripPlanResponseDTO response = tripPlanFacade.createTripPlan(request);

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
        TripPlanDetailResponseDTO response = tripPlanFacade.getTripPlanDetail(planId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 여행 플랜 수정
     */
    @Override
    @PatchMapping("/{planId}")
    public ResponseEntity<TripPlanResponseDTO> putPlan(
            @PathVariable Long planId,
            @RequestBody TripPlanPatchRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        TripPlanResponseDTO updatedPlan = tripPlanFacade.updatePlan(userId, planId, request);

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
        TripPlanResponseDTO response = tripPlanFacade.leaveTripPlan(planId, userId);

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

}
