package com.project.moneyj.trip.plan.service;

import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.dto.category.CategoryDTO;
import com.project.moneyj.trip.member.service.CategoryService;
import com.project.moneyj.trip.member.service.TripMemberService;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.dto.TripPlanDetailResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.plan.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.plan.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.tip.service.SavingTipService;
import com.project.moneyj.trip.tip.service.TripTipService;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TripPlanFacade {
    private final TripPlanService tripPlanService;
    private final TripMemberService tripMemberService;
    private final TripTipService tripTipService;
    private final CategoryService categoryService;

    private final UserService userService;
    private final AccountService accountService;
    private final SavingTipService savingTipService;


    @Transactional
    public TripPlanResponseDTO createTripPlan(TripPlanRequestDTO request) {
        List<User> members = userService.getUsersByEmails(request.getTripMemberEmail());

        TripPlan tripPlan = tripPlanService.createTripPlan(members.size(), request);
        tripMemberService.enrollMembersWithCategories(
            tripPlan,
            members,
            request.getCategoryDTOList()
        );

        return new TripPlanResponseDTO(tripPlan.getTripPlanId(), "여행 플랜 생성 완료");
    }

    @Transactional
    public TripPlanDetailResponseDTO getTripPlanDetail(Long planId, Long userId) {
        savingTipService.checkSavingTip(userId, planId);

        TripPlan plan = tripPlanService.getTripPlan(planId);
        TripMember member = tripMemberService.getTripMember(planId, userId);

        List<String> savingsTips = savingTipService.getSavingsTips(userId);
        List<String> tripTips = tripTipService.getSavingsTips(plan.getCountry());
        List<CategoryDTO> categories = categoryService.getCategories(member, planId);

        return TripPlanDetailResponseDTO.fromEntity(plan, savingsTips, tripTips, categories);
    }

    @Transactional
    public TripPlanResponseDTO leaveTripPlan(Long planId, Long userId) {
        User user = userService.getUser(userId);
        TripPlan tripPlan = tripPlanService.getTripPlanWithLock(planId);
        accountService.deleteAccountByTripPlanAndUser(tripPlan, user);

        TripMember member = tripMemberService.getTripMember(planId, userId);
        tripPlanService.leaveTripPlan(tripPlan, member);

        return new TripPlanResponseDTO(planId, "여행 플랜 탈퇴 완료");
    }

    @Transactional
    public TripPlanResponseDTO updatePlan(Long userId, Long planId, TripPlanPatchRequestDTO request) {
        tripMemberService.validateMember(userId, planId);
        tripPlanService.updatePlan(planId, request);

        return new TripPlanResponseDTO(planId, "여행 플랜 수정 완료");
    }

}
