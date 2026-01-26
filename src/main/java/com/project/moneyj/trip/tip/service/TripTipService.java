package com.project.moneyj.trip.tip.service;


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
import com.project.moneyj.trip.tip.domain.TripSavingPhrase;
import com.project.moneyj.trip.tip.dto.SavingsTipResponseDTO;
import com.project.moneyj.trip.tip.repository.TripSavingPhraseRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripTipService {

    private final UserRepository userRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;

    private final ChatClient chatClient;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionSummaryService transactionSummaryService;


    /**
     * 저축 팁 관련 Prompt 및 저축 Tip 생성
     */
    @Transactional
    public void addSavingsTip(Long userId, Long planId) {

        if (tripSavingPhraseRepository.existsByUserIdAndPlanId(userId, planId)) {
            return;
        }

        // 1. 현재 저축 금액 조회
        int currentSavings = accountService.getUserBalance(userId, planId);

        // 2. 여행 플랜 예산 조회 (목표 저축 금액)
        TripPlan tripPlan = tripPlanRepository.findById(planId)
            .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));
        int tripBudget = tripPlan.getTotalBudget();

        // 3. 최근 6개월 소비 내역 요약
        String baseYearMonth = YearMonth.now().toString(); // 예: "2025-09"
        List<MonthlySummaryDTO> summaries = transactionSummaryService.getMonthlySummary(userId, baseYearMonth);

        // 4. TripMember 조회
        TripMember tripMember = tripMemberRepository.findByUserIdAndPlanId(userId, planId)
            .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

        // 소비내역 요약이 없을 경우 기본값 저장
        if(summaries == null || summaries.isEmpty()) {
            TripSavingPhrase phrase = TripSavingPhrase.of(
                tripMember,
                "카드에 소비내역이 없어서 저축 팁을 생성하지 못했어요. 나중에 다시 확인해 보세요!"
            );

            tripSavingPhraseRepository.save(phrase);
            return;
        }

        Map<String, Integer> categoryTotals = new HashMap<>();

        for (MonthlySummaryDTO monthSummary : summaries) {
            for (MonthlySummaryDTO.CategorySummaryDTO cat : monthSummary.getCategories()) {
                categoryTotals.merge(cat.getCategory(), cat.getTotalAmount(), Integer::sum);
            }
        }

        // 카테고리별 평균 소비액 계산
        Map<String, Integer> categoryAverages = categoryTotals.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() / summaries.size() // 6개월 평균
            ));

        // 프롬프트용 문자열 (평균 소비 상위 5개 카테고리)
        String transactionSummary = categoryAverages.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> String.format("%s : %d", e.getKey(), e.getValue()))
            .collect(Collectors.joining("\n"));

        // 5. 프롬프트 작성
        String promptTemplate = PromptLoader.load("/prompts/savings-tip.txt");
        String promptText = String.format(
            promptTemplate,
            currentSavings,   // 현재 저축 금액
            tripBudget,       // 목표 저축 금액
            transactionSummary // 6개월 평균 소비 내역
        );

        // 6. GPT 호출
        SavingsTipResponseDTO response = chatClient
            .prompt()
            .system("너는 저축 조언 전문가야. " +
                "사용자의 소비 내역을 분석해서 반드시 3개의 맞춤형 저축 팁을 작성해줘. \\\n" +
                "반드시 예시를 참고하여 구어체를 사용하여 답변해.")
            .user(promptText)
            .call()
            .entity(SavingsTipResponseDTO.class);


        for (String tip : response.getMessages()) {
            TripSavingPhrase phrase = TripSavingPhrase.of(tripMember, tip);
            tripSavingPhraseRepository.save(phrase);
        }
    }


    @Transactional
    public void checkSavingTip(Long userId, Long planId) {

        //트랜잭션 잠금
        TripMember member = tripMemberRepository.findMemberForUpdate(userId, planId);

        // 1. 기존 저축 팁 존재 여부 확인
        if (tripSavingPhraseRepository.existsByUserIdAndPlanId(userId, planId)) {
            return;
        }

        // 2. 플랜 참여 여부 확인
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, planId)) {
            return;
        }

        // 3. 계좌 확인
        if (accountRepository.findByUserIdAndTripPlanId(userId, planId).isEmpty()){
           return;
       }

        // 4. 카드 확인
        if (!userRepository.findByUserId(userId)
                .map(User::isCardConnected)
                .orElse(false)){
            return;
        }

        // 5. 네 조건이 모두 true일 때만 실행
        addSavingsTip(userId, planId);
    }

    @Transactional
    //Todo : updateSavingsTip, checkSavingsTip(트랜잭션 잠금용) 코드 중복 정리
    public void updateSavingsTip(Long userId, Long planId) {
        if (!tripMemberRepository.existsMemberByUserAndPlan(userId, planId)) {
            return;
        }

        if (accountRepository.findByUserIdAndTripPlanId(userId, planId).isEmpty()){
            return;
        }

        if (!userRepository.findByUserId(userId)
            .map(User::isCardConnected)
            .orElse(false)){
            return;
        }

        addSavingsTip(userId, planId);
    }

}

