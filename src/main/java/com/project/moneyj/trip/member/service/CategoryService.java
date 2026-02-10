package com.project.moneyj.trip.member.service;

import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CategoryErrorCode;
import com.project.moneyj.exception.code.TripMemberErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.trip.member.domain.Category;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.dto.category.CategoryDTO;
import com.project.moneyj.trip.member.dto.category.CategoryListRequestDTO;
import com.project.moneyj.trip.member.dto.category.CategoryResponseDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedRequestDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedResponseDTO;
import com.project.moneyj.trip.member.repository.CategoryRepository;
import com.project.moneyj.trip.member.repository.TripMemberRepository;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final TripMemberRepository tripMemberRepository;
    private final TripPlanRepository tripPlanRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;


    /**
     * 카테고리 리스트 조회
     */
    public List<CategoryDTO> getCategories(TripMember member, Long planId) {
        return member.getCategoryList().stream()
            .map(c -> CategoryDTO.fromEntity(c, planId))
            .toList();
    }

    /**
     * 여행 플랜 카테고리 목표 달성 여부 변경 메소드
     */
    @Transactional
    public isConsumedResponseDTO switchIsConsumed(isConsumedRequestDTO request, Long userId) {

        // TODO: tripPlan 조회 안하고 tripMemberRepository.findByTripPlanAndUser 여기서 객체가 아니라 id로 조회하도록 변경
        TripPlan tripPlan = tripPlanRepository.findByTripPlanId(request.getTripPlanId());
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(tripPlan, user)
            .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

        Category category = categoryRepository.findByCategoryNameAndMemberIdNative(request.getCategoryName(), tripMember.getTripMemberId())
            .orElseThrow(() -> MoneyjException.of(CategoryErrorCode.NOT_FOUND));

        category.changeConsumptionStatus(request.isConsumed());

        return new isConsumedResponseDTO("카테고리 목표 달성 여부가 반영 되었습니다.", category.isConsumed());
    }


    /**
     * 여행 플랜 카테고리 목표 달성 조회
     */
    @Transactional
    public List<CategoryDTO> getIsConsumed(Long planId, Long userId) {

        TripPlan tripPlan = tripPlanRepository.findByTripPlanId(planId);
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(tripPlan, user)
            .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

        List<Category> categoriesList = categoryRepository.findByTripPlanIdAndTripMemberId(planId, tripMember.getTripMemberId());

        return categoriesList.stream().map(category -> CategoryDTO.fromEntity(category, planId)).toList();
    }

    /**
     * 카테고리 변경
     * 한명이 변경하면 모든 사용자의 카테고리 변경
     */
    // TODO: 여러 멤버가 동시에 수정하는 경우 락 도입 고려
    @Transactional
    public CategoryResponseDTO patchCategory(CategoryListRequestDTO request, Long userId) {

        // 사용자 검증 (실제 여행 멤버에 속하는지)
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, request.getCategoryDTOList().get(0).getTripPlanId())) {
            throw MoneyjException.of(TripMemberErrorCode.NOT_FOUND);
        }

        TripPlan tripPlan = tripPlanRepository.findByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());

        List<TripMember> tripMemberList = tripMemberRepository.findTripMemberByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());


        // TODO: IN절 사용해서 쿼리 개수 줄이기 (N+1 문제 방지)
        // 카테고리 전체 순환
        for (CategoryDTO categoryDTO : request.getCategoryDTOList()) {

            // 여행 멤버 전체 순환
            for (TripMember tripMember : tripMemberList) {

                Category category = categoryRepository.findByCategoryNameAndMemberIdNative(categoryDTO.getCategoryName(), tripMember.getTripMemberId())
                    .orElseThrow(() -> MoneyjException.of(CategoryErrorCode.NOT_FOUND));

                category.update(categoryDTO);
            }

        }

        // TODO: sum 계산시 +=로 누적
        Integer sum = 0;
        for (CategoryDTO categoryDTO : request.getCategoryDTOList()) sum = categoryDTO.getAmount();

        tripPlan.updateTotalBudget(sum);

        return new CategoryResponseDTO(
            "여행 멤버들의 카테고리가 변경 되었습니다.",
            request.getCategoryDTOList().get(0).getCategoryName(),
            request.getCategoryDTOList().get(0).getAmount()
        );
    }


}
