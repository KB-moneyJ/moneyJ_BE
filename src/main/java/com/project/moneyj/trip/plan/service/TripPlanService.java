package com.project.moneyj.trip.plan.service;


import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.openai.util.PromptLoader;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.dto.TripBudgetRequestDTO;
import com.project.moneyj.trip.plan.dto.TripBudgetResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanListResponseDTO;
import com.project.moneyj.trip.plan.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.plan.dto.TripPlanQueryDTO;
import com.project.moneyj.trip.plan.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.trip.plan.repository.query.TripPlanQuerydslRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final TripPlanRepository tripPlanRepository;
    private final TripPlanQuerydslRepository tripPlanQuerydslRepository;

    private final ChatClient chatClient;

    /**
     * 여행 플랜 생성
     */
    @Transactional
    public TripPlan createTripPlan(int memberCount, TripPlanRequestDTO dto) {
        TripPlan tripPlan = TripPlan.of(
            memberCount,
            dto.getCountry(),
            dto.getCountryCode(),
            dto.getCity(),
            dto.getDays(),
            dto.getNights(),
            dto.getTripStartDate(),
            dto.getTripEndDate(),
            dto.getTotalBudget(),
            dto.getStartDate(),
            dto.getTargetDate()
        );

        return tripPlanRepository.save(tripPlan);
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

    private double calcProgress(Long totalBalance, Integer budget) {
        if (budget == null || budget <= 0) return 0.0;

        double raw = (totalBalance * 100.0) / budget;
        return BigDecimal.valueOf(raw)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();
    }

    /**
     * 조회용 여행 플랜 조회
     */
    @Transactional(readOnly = true)
    public TripPlan getTripPlan(Long planId) {
        TripPlan plan = tripPlanRepository.findDetailById(planId)
            .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        if (plan.hasNoMembers()) {
            throw MoneyjException.of(TripPlanErrorCode.NO_MEMBERS_IN_PLAN);
        }

        return plan;
    }

    /**
     * 쓰기용 여행 플랜 리스트 조회
     */
    @Transactional
    public TripPlan getTripPlanWithLock(Long planId) {
        return tripPlanRepository.findByIdWithPessimisticLock(planId)
            .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));
    }

    /**
     * 여행 플랜 수정 (카테고리 제외)
     */
    @Transactional
    public void updatePlan(Long planId, TripPlanPatchRequestDTO request) {
        TripPlan plan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        plan.update(request);
    }

    /**
     * 여행 플랜 탈퇴
     */
    @Transactional
    public void leaveTripPlan(TripPlan plan, TripMember member) {
        plan.removeMember(member);

        if (plan.hasNoMembers()) {
            tripPlanRepository.delete(plan);
        }
    }

    // TODO: ai 관련 서비스로 아예 분리할지
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

}

