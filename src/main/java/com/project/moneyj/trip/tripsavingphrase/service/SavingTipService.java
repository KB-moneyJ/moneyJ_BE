package com.project.moneyj.trip.tripsavingphrase.service;


import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.TripMemberErrorCode;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.openai.util.PromptLoader;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.repository.TripMemberRepository;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.trip.tripsavingphrase.domain.TripSavingPhrase;
import com.project.moneyj.trip.tripsavingphrase.dto.SavingsTipResponseDTO;
import com.project.moneyj.trip.tripsavingphrase.repository.TripSavingPhraseRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingTipService {

    private final UserRepository userRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;

    private final ChatClient chatClient;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionSummaryService transactionSummaryService;

    /**
     * 저축팁 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<String> getSavingsTips(Long memberId) {
        return tripSavingPhraseRepository.findAllContentByTripMemberId(memberId);
    }

    /**
     * 저축 팁 관련 Prompt 및 저축 Tip 생성
     */
    @Transactional
    public void addSavingsTip(Long userId, Long planId) {

        int currentSavings = accountService.getUserBalance(userId, planId);

        TripPlan tripPlan = tripPlanRepository.findById(planId)
            .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        int tripBudget = tripPlan.getTotalBudget();

        TripMember tripMember = tripMemberRepository.findByUserIdAndPlanId(userId, planId)
            .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

       String transactionSummary = buildCategorySummaryText(userId);

        // 소비내역 요약이 없을 경우 기본값 저장
        if (transactionSummary.isEmpty()) {
            TripSavingPhrase phrase = TripSavingPhrase.of(
                    tripMember,
                    "카드에 소비내역이 없어서 저축 팁을 생성하지 못했어요. 나중에 다시 확인해 보세요!"
            );
            tripSavingPhraseRepository.save(phrase);
            return;
        }
        LocalDate startDate = tripPlan.getStartDate();

        LocalDate now = LocalDate.now();

        long daysUntilTrip = ChronoUnit.DAYS.between(now, startDate);

        String promptText = buildPrompt(currentSavings, tripBudget, transactionSummary, now, startDate, daysUntilTrip);

        SavingsTipResponseDTO response = callGpt(promptText);

        for (String tip : response.getMessages()) {
            TripSavingPhrase phrase = TripSavingPhrase.of(tripMember, tip);
            tripSavingPhraseRepository.save(phrase);
        }
    }
    public String buildCategorySummaryText(Long userId) {
        String baseYearMonth = YearMonth.now().toString();
        List<MonthlySummaryDTO> summaries = transactionSummaryService.getMonthlySummary(userId, baseYearMonth);

        if (summaries == null || summaries.isEmpty()) {
            return "";
        }

        Map<String, Integer> categoryTotals = new HashMap<>();

        for (MonthlySummaryDTO monthSummary : summaries) {
            for (MonthlySummaryDTO.CategorySummaryDTO cat : monthSummary.getCategories()) {
                categoryTotals.merge(cat.getCategory(), cat.getTotalAmount(), Integer::sum);
            }
        }

        if (categoryTotals.isEmpty()) {
            return "";
        }

        Map<String, Integer> categoryAverages = categoryTotals.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / summaries.size() // 6개월 평균
                ));

        // 프롬프트용 문자열 (평균 소비 상위 5개 카테고리)
        return  categoryAverages.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> String.format("%s : %d", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    public String buildPrompt (
            int currentSavings,
            int tripBudget,
            String transactionSummary,
            LocalDate now,
            LocalDate startDate,
            long daysUntilTrip
    ) {
        String promptTemplate = PromptLoader.load("/prompts/savings-tip.txt");
        return String.format(
                promptTemplate,
                currentSavings, // 현재 저축 금액
                tripBudget, // 목표 저축 금액
                transactionSummary,// 6개월 평균 소비 내역
                now.toString(), // 현재 날짜
                startDate.toString(), // 여행 출발 날짜
                daysUntilTrip //현재 날짜 ~ 여행 출발 날짜까지의 차이 일
        );
    }

    public SavingsTipResponseDTO callGpt(String promptText) {
        return chatClient
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .maxCompletionTokens(1000)
                        .temperature(0.0)
                        .build())
                .system("너는 저축 조언 전문가야. " +
                        "사용자의 소비 내역을 분석해서 반드시 3개의 맞춤형 저축 팁을 작성해줘. \\\n" +
                        "반드시 예시를 참고하여 구어체를 사용하여 답변해.")
                .user(promptText)
                .call()
                .entity(SavingsTipResponseDTO.class);
    }


    // TODO: 트랜잭션 잠금 필요한지 & 되고 있는지 확인 필요
    @Transactional
    public void checkSavingTip(Long userId, Long planId) {
        //트랜잭션 잠금
        TripMember member = tripMemberRepository.findMemberForUpdate(userId, planId);

        //기존 저축 팁 생성 여부 확인
        if (tripSavingPhraseRepository.existsByUserIdAndPlanId(userId, planId)) {
            return;
        }
        if (conditionSavingsTip(userId, planId)) {
            addSavingsTip(userId, planId);
        }
    }

    @Transactional
    public void updateSavingsTip(Long userId, Long planId, Long memberId) {
        tripSavingPhraseRepository.deleteByTripMember_TripMemberId(memberId);

        if (conditionSavingsTip(userId, planId)) {
            addSavingsTip(userId, planId);
        }
    }

    private boolean conditionSavingsTip(Long userId, Long planId) {
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, planId)) return false;
        if (accountRepository.findByUserIdAndTripPlanId(userId, planId).isEmpty()) return false;
        return userRepository.findByUserId(userId)
                .map(User::isCardConnected)
                .orElse(false);
    }
}

