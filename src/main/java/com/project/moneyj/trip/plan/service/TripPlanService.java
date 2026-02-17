package com.project.moneyj.trip.plan.service;


import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CategoryErrorCode;
import com.project.moneyj.exception.code.TripMemberErrorCode;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.openai.util.PromptLoader;
import com.project.moneyj.trip.member.domain.MemberRole;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.repository.TripMemberRepository;
import com.project.moneyj.trip.plan.domain.Category;
import com.project.moneyj.trip.plan.domain.TripPlan;
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
import com.project.moneyj.trip.plan.dto.plan.TripPlanQueryDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanRequestDTO;
import com.project.moneyj.trip.plan.dto.plan.TripPlanResponseDTO;
import com.project.moneyj.trip.plan.repository.CategoryRepository;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.trip.plan.repository.query.TripPlanQuerydslRepository;
import com.project.moneyj.trip.tip.repository.TripSavingPhraseRepository;
import com.project.moneyj.trip.tip.repository.TripTipRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripPlanService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final TripPlanRepository tripPlanRepository;
    private final TripPlanQuerydslRepository tripPlanQuerydslRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripTipRepository tripTipRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;

    private final ChatClient chatClient;
    private final AccountRepository accountRepository;

    private final Clock clock;

    /**
     * 여행 플랜 생성
     */
    @Transactional
    public TripPlanResponseDTO createTripPlans(TripPlanRequestDTO requestDTO) {

        // 멤버들 id 조회
        List<User> members = userRepository.findAllByEmailIn(requestDTO.getTripMemberEmail());

        TripPlan tripPlan = TripPlan.of(
                members.size(),
                requestDTO.getCountry(),
                requestDTO.getCountryCode(),
                requestDTO.getCity(),
                requestDTO.getDays(),
                requestDTO.getNights(),
                requestDTO.getTripStartDate(),
                requestDTO.getTripEndDate(),
                requestDTO.getTotalBudget(),
                requestDTO.getStartDate(),
                requestDTO.getTargetDate());

        TripPlan saved = tripPlanRepository.save(tripPlan);

        // 모든 멤버 등록
        for (User user : members) {
            TripMember tripMember = TripMember.of(null,null, MemberRole.MEMBER);
            tripMember.enrollTripMember(user, saved);

            // 모든 멤버 같은 카테고리 및 금액 등록
            for (CategoryDTO categoryDTO : requestDTO.getCategoryDTOList()) {
                Category category = Category.of(
                                categoryDTO.getCategoryName(),
                                categoryDTO.getAmount(),
                                false,
                                saved,
                                tripMember);

                tripMember.getCategoryList().add(category);
            }

            tripMemberRepository.save(tripMember);
        }

        return new TripPlanResponseDTO(saved.getTripPlanId(), "여행 플랜 생성 완료");
    }

    /**
     * 여행 플랜 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlanListResponseDTO> getUserTripPlans(Long userId) {

        List<TripPlanQueryDTO> tripPlans = tripPlanQuerydslRepository.findAllWithProgress(userId);

        return tripPlans.stream()
            .map(tp -> {
                double progress = calcProgress(tp.getTotalBalance(), tp.getTotalBudget() * tp.getMembersCount());
                return TripPlanListResponseDTO.of(tp, progress);
            })
            .toList();
    }

    /**
     * 여행 플랜 상세 조회
     */
    @Transactional(readOnly = true)
    public TripPlanDetailResponseDTO getTripPlanDetail(Long planId, Long userId) {

        // 여행 플랜 조회
        TripPlan plan = tripPlanRepository.findDetailById(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        // TripMember 가 한 명도 없는 경우
        if (plan.getTripMemberList().isEmpty()) {
            throw MoneyjException.of(TripPlanErrorCode.NO_MEMBERS_IN_PLAN);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(plan, user)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행 멤버: " + user.getEmail()));
        ;


        // 문구 조회
        // 저축 플랜 문구
        List<String> savings = tripSavingPhraseRepository.findAllContentByMemberId(userId);
        if (savings.isEmpty()) savings = new ArrayList<>();

        // 여행 팁 문구
        List<String> tips = tripTipRepository.findAllByCountry(plan.getCountry());
        if (tips.isEmpty()) tips = new ArrayList<>();

        // 카테고리 조회 및 DTO 변환
        List<Category> categoryList = tripMember.getCategoryList();

        List<CategoryDTO> categoryDTOList = categoryList.isEmpty() ? new ArrayList<>() : categoryList.stream()
                .map(category -> CategoryDTO.fromEntity(category, planId))
                .toList();


        return TripPlanDetailResponseDTO.fromEntity(plan, savings, tips, categoryDTOList);
    }

    /**
     * 여행 플랜 수정 (카테고리 제외)
     */
    @Transactional
    public TripPlanResponseDTO patchPlan(Long userId, Long planId, TripPlanPatchRequestDTO requestDTO) {

        // 사용자 검증 (실제 여행 멤버에 속하는지)
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, planId)) {
            throw MoneyjException.of(TripMemberErrorCode.NOT_FOUND);
        }

        TripPlan existingPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        existingPlan.update(requestDTO);

        return new TripPlanResponseDTO(planId, "여행 플랜 수정하였습니다.");
    }

    /**
     * 여행 플랜 삭제
     */
    @Transactional
    public TripPlanResponseDTO leavePlan(Long planId, Long currentUserId) {

        // 사용자가 존재하는지 확인
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        // 해당 여행 플랜이 존재하는지 확인 (동시 삭제/수정 막기 위한 비관락 조회)
        TripPlan tripPlan = tripPlanRepository.findByIdWithPessimisticLock(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        // 사용자가 해당 플랜의 멤버인지 확인
        TripMember memberToRemove = tripMemberRepository.findByTripPlanAndUser(tripPlan, currentUser)
                .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

        // 계좌 삭제
        accountRepository.deleteByTripPlanAndUser(tripPlan, currentUser);
        accountRepository.flush();

        // 멤버 제거 및 카운트 갱신 (orphanRemoval에 의해 TripMember, Category, Phrase 삭제됨)
        tripPlan.removeMember(memberToRemove);

        // 모든 멤버가 탈퇴된 경우
        if (tripPlan.getTripMemberList().isEmpty()) {
            tripPlanRepository.delete(tripPlan);
            return new TripPlanResponseDTO(planId, "마지막 멤버가 탈퇴하여 플랜이 삭제되었습니다.");
        }

        return new TripPlanResponseDTO(planId, "해당 플랜에서 탈퇴했습니다.");
    }

    /**
     * 여행 경비 계산 관련 Prompt
     */
    public TripBudgetResponseDTO getTripBudget(TripBudgetRequestDTO request) {

        String promptTemplate = PromptLoader.load("/prompts/trip_budget.txt");

        String promptText = String.format(
                promptTemplate,
                request.getCountry(),
                request.getCity(),
                request.getNights(),
                request.getDays(),
                request.getStartDate(),
                request.getEndDate()
        );

        return chatClient
                .prompt()
                .system("너는 여행 경비 분석가야. 반드시 JSON으로만 답변해야 한다.")
                .user(promptText)
                .call()
                .entity(TripBudgetResponseDTO.class);
    }


    /**
     * 여행 플랜 카테고리 목표 달성 여부 변경 메소드
     */
    @Transactional
    public isConsumedResponseDTO switchIsConsumed(isConsumedRequestDTO request, Long userId) {

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
    @Transactional
    public CategoryResponseDTO patchCategory(CategoryListRequestDTO request, Long userId) {

        // 사용자 검증 (실제 여행 멤버에 속하는지)
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, request.getCategoryDTOList().get(0).getTripPlanId())) {
            throw MoneyjException.of(TripMemberErrorCode.NOT_FOUND);
        }

        TripPlan tripPlan = tripPlanRepository.findByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());

        List<TripMember> tripMemberList = tripMemberRepository.findTripMemberByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());


        // 카테고리 전체 순환
        for (CategoryDTO categoryDTO : request.getCategoryDTOList()) {

            // 여행 멤버 전체 순환
            for (TripMember tripMember : tripMemberList) {

                Category category = categoryRepository.findByCategoryNameAndMemberIdNative(categoryDTO.getCategoryName(), tripMember.getTripMemberId())
                        .orElseThrow(() -> MoneyjException.of(CategoryErrorCode.NOT_FOUND));

                category.update(categoryDTO);
            }

        }

        Integer sum = 0;
        for (CategoryDTO categoryDTO : request.getCategoryDTOList()) sum = categoryDTO.getAmount();

        tripPlan.updateTotalBudget(sum);

        return new CategoryResponseDTO(
                "여행 멤버들의 카테고리가 변경 되었습니다.",
                request.getCategoryDTOList().get(0).getCategoryName(),
                request.getCategoryDTOList().get(0).getAmount()
        );
    }

    private double calcProgress(Long totalBalance, Integer budget) {
        if (budget == null || budget <= 0) return 0.0;

        double raw = (totalBalance * 100.0) / budget;
        return BigDecimal.valueOf(raw)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();
    }

}

