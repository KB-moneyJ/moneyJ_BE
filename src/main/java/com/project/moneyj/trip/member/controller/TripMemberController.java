package com.project.moneyj.trip.member.controller;


import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.member.dto.AddTripMemberRequestDTO;
import com.project.moneyj.trip.member.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.member.service.TripMemberService;
import com.project.moneyj.trip.plan.dto.plan.TripPlanResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/trip-plans")
public class TripMemberController implements TripMemberControllerApiSpec {

    private final TripMemberService tripMemberService;

    /**
     * 여행 멤버 추가
     */
    @Override
    @PostMapping("/{planId}/members")
    public ResponseEntity<TripPlanResponseDTO> addTripMember(
            @PathVariable Long planId,
            @RequestBody AddTripMemberRequestDTO addTripMemberRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        TripPlanResponseDTO updatedPlan = tripMemberService.addTripMember(userId, planId, addTripMemberRequestDTO);
        return ResponseEntity.ok(updatedPlan);
    }

    /**
     * 여행 멤버별 저축 금액 및 달성률
     * 마지막 동기화 < 3시간 -> DB에서 바로 금액 반환
     * 마지막 동기화 >= 3시간: CODEF 호출
     */
    @Override
    @GetMapping("/{tripPlanId}/balances")
    public ResponseEntity<UserBalanceResponseDTO> getBalances(
        @PathVariable Long tripPlanId,
        @AuthenticationPrincipal CustomOAuth2User customUser
    ) {
        Long userId = customUser.getUserId();
        UserBalanceResponseDTO response = tripMemberService.getUserBalances(userId, tripPlanId);
        return ResponseEntity.ok(response);
    }


}
