package com.project.moneyj.trip.service;


import com.project.moneyj.account.Service.AccountService;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.openai.util.PromptLoader;
import com.project.moneyj.trip.domain.*;
import com.project.moneyj.trip.dto.*;
import com.project.moneyj.trip.repository.*;
import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.trip.dto.TripPlanDetailResponseDTO;
import com.project.moneyj.trip.dto.TripPlanListResponseDTO;
import com.project.moneyj.trip.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.dto.TripBudgetResponseDTO;
import com.project.moneyj.trip.dto.TripBudgetRequestDTO;
import com.project.moneyj.trip.repository.TripMemberRepository;
import com.project.moneyj.trip.repository.TripPlanRepository;
import com.project.moneyj.trip.repository.TripSavingPhraseRepository;
import com.project.moneyj.trip.repository.TripTipRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
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
public class TripPlanService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripTipRepository tripTipRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;
    private final AccountRepository accountRepository;
    private final ChatClient chatClient;
    private final AccountService accountService;
    private final TransactionSummaryService transactionSummaryService;

    /**
     * 여행 플랜 생성
     */
    @Transactional
    public TripPlanResponseDTO createTripPlans(TripPlanRequestDTO requestDTO) {

        // 멤버들 id 조회
        List<User> members = userRepository.findAllByEmailIn(requestDTO.getTripMemberEmail());

        TripPlan tripPlan = TripPlan.builder()
                .country(requestDTO.getCountry())
                .countryCode(requestDTO.getCountryCode())
                .city(requestDTO.getCity())
                .membersCount(members.size())
                .days(requestDTO.getDays())
                .nights(requestDTO.getNights())
                .tripStartDate(requestDTO.getTripStartDate())
                .tripEndDate(requestDTO.getTripEndDate())
                .totalBudget(requestDTO.getTotalBudget())
                .startDate(requestDTO.getStartDate())
                .targetDate(requestDTO.getTargetDate())
                .build();

        TripPlan saved = tripPlanRepository.save(tripPlan);

        // 모든 멤버 등록
        for (User user : members) {
            TripMember tripMember = new TripMember();
            tripMember.enrollTripMember(user, saved);

            // 모든 멤버 같은 카테고리 및 금액 등록
            for (CategoryDTO categoryDTO : requestDTO.getCategoryDTOList()) {
                Category category = Category.builder()
                        .categoryName(categoryDTO.getCategoryName())
                        .amount(categoryDTO.getAmount())
                        .tripPlan(saved)
                        .tripMember(tripMember)
                        .build();

                tripMember.getCategoryList().add(category);
            }

            tripMemberRepository.save(tripMember);
        }

        return new TripPlanResponseDTO(saved.getTripPlanId(), "여행 플랜 생성 완료");
    }

    /**
     * 여행 플랜 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlanListResponseDTO> getUserTripPlans(Long userId) {

        List<TripPlan> tripPlan = tripPlanRepository.findAllByUserId(userId);
        return tripPlan.stream()
                .map(TripPlanListResponseDTO::fromEntity)
                .toList();
    }

    /**
     * 여행 플랜 상세 조회
     */
    @Transactional(readOnly = true)
    public TripPlanDetailResponseDTO getTripPlanDetail(Long planId, Long userId) {

        // 여행 플랜 조회
        TripPlan plan = tripPlanRepository.findDetailById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 플랜"));

        // TripMember 가 한 명도 없는 경우
        if (plan.getTripMemberList().isEmpty()) {
            throw new IllegalArgumentException("해당 플랜에 멤버가 존재하지 않습니다!");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(plan, user)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행 멤버: " + user.getEmail()));
        ;

        // Category 가 한 개도 없는 경우
        if (tripMember.getCategoryList().isEmpty()) {
            throw new IllegalArgumentException("해당 플랜에 아직 카테고리가 없습니다.");
        }

        // 문구 조회
        // 저축 플랜 문구
        List<String> savings = tripSavingPhraseRepository.findAllContentByMemberId(userId);
        if (savings.isEmpty()) savings = new ArrayList<>();

        // 여행 팁 문구
        List<String> tips = tripTipRepository.findAllByCountry(plan.getCountry());
        if (tips.isEmpty()) tips = new ArrayList<>();

        // 카테고리 조회 및 DTO 변환
        List<Category> categoryList = tripMember.getCategoryList();
        List<CategoryDTO> categoryDTOList = categoryList.stream()
                .map(category -> CategoryDTO.fromEntity(category, planId))
                .toList();


        return TripPlanDetailResponseDTO.fromEntity(plan, savings, tips, categoryDTOList);
    }

    /**
     * 여행 플랜 수정 (카테고리 제외)
     */
    @Transactional
    public TripPlanResponseDTO patchPlan(Long planId, TripPlanPatchRequestDTO requestDTO) {

        TripPlan existingPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행 플랜을 찾을 수 없습니다!: " + planId));

        existingPlan.update(requestDTO);

        return new TripPlanResponseDTO(planId, "여행 플랜 수정하였습니다.");
    }

    /**
     * 여행 멤버 추가
     */
    @Transactional
    public TripPlanResponseDTO addTripMember(Long planId, AddTripMemberRequestDTO addDTO) {

        // 여행 플랜 조회
        TripPlan existingPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행 플랜을 찾을 수 없습니다!" + planId));


        // 여행 플랜 카테고리
        TripMember tripMember = tripMemberRepository.findTripMemberByTripPlanId(planId).get(0);

        List<Category> categoryList = categoryRepository.findByTripPlanIdAndTripMemberId(planId, tripMember.getTripMemberId());

        // 사용자 조회
        for (String email : addDTO.getEmail()) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다!" + email));

            if (tripMemberRepository.findByTripPlanAndUser(existingPlan, user).isPresent()) {
                throw new IllegalArgumentException("이미 여행에 참여하고 있는 멤버입니다! " + email);
            }


            TripMember addTripMember = TripMember.builder()
                    .user(user)
                    .memberRole(MemberRole.MEMBER)
                    .build();

            addTripMember.addTripMember(existingPlan);

            for (Category category : categoryList) {
                Category newCategory = Category.builder()
                        .tripMember(addTripMember)
                        .tripPlan(existingPlan)
                        .categoryName(category.getCategoryName())
                        .amount(category.getAmount())
                        .build();
                categoryRepository.save(newCategory);
            }

        }

        List<TripMember> tripMemberList = tripMemberRepository.findTripMemberByTripPlanId(planId);
        existingPlan.updateMembersCount(tripMemberList.size());

        return new TripPlanResponseDTO(planId, "멤버 추가 완료");

    }

    /**
     * 여행 플랜 삭제
     */
    @Transactional
    public TripPlanResponseDTO leavePlan(Long planId, Long currentUserId) {

        // 사용자가 존재하는지 확인
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("가입된 사용자가 아닙니다."));

        // 해당 여행 플랜이 존재하는지 확인
        TripPlan tripPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 플랜입니다."));

        // 사용자가 해당 플랜의 멤버인지 확인
        TripMember memberToRemove = tripMemberRepository.findByTripPlanAndUser(tripPlan, currentUser)
                .orElseThrow(() -> new IllegalStateException("해당 여행 플랜의 멤버가 아닙니다."));

        // TripMember 삭제
        // 고아 객체 옵션에 의해 TripPlan 의 tripMemberList 에서도 자동으로 제거 됨.
        tripMemberRepository.delete(memberToRemove);

        return new TripPlanResponseDTO(planId, "해당 플랜을 삭제하였습니다.");
    }

    @Transactional(readOnly = true)
    public List<UserBalanceResponseDTO> getUserBalances(Long tripPlanId) {
        List<Account> accounts = accountRepository.findByTripPlanId(tripPlanId);

        return accounts.stream()
                .map(a -> {
                    double rawProgress = 0.0;
                    TripPlan tp = a.getTripPlan();
                    if (tp != null && tp.getTotalBudget() != null && tp.getTotalBudget() > 0) {
                        rawProgress = (a.getBalance() * 100.0) / tp.getTotalBudget();
                    }
                    // 소수점 1자리로 반올림
                    double progress = new BigDecimal(rawProgress)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

                    return new UserBalanceResponseDTO(
                            a.getUser().getUserId(),
                            a.getUser().getNickname(),
                            a.getUser().getProfileImage(),
                            a.getBalance(),
                            progress
                    );
                })
                .toList();
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다!"));

        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(tripPlan, user)
                .orElseThrow(() -> new IllegalArgumentException("현재 사용자는 해당 여행의 멤버가 아닙니다!"));

        Category category = categoryRepository.findByCategoryNameAndMemberIdNative(request.getCategoryName(), tripMember.getTripMemberId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다!"));

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다!"));

        TripMember tripMember = tripMemberRepository.findByTripPlanAndUser(tripPlan, user)
                .orElseThrow(() -> new IllegalArgumentException("현재 사용자는 해당 여행의 멤버가 아닙니다!"));

        List<Category> categoriesList = categoryRepository.findByTripPlanIdAndTripMemberId(planId, userId);

        return categoriesList.stream().map(category -> CategoryDTO.fromEntity(category, planId)).toList();
    }

    /**
     * 카테고리 변경
     * 한명이 변경하면 모든 사용자의 카테고리 변경
     */
    @Transactional
    public CategoryResponseDTO patchCategory(CategoryListRequestDTO request, Long userId) {

        TripPlan tripPlan = tripPlanRepository.findByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다!"));

        List<TripMember> tripMemberList = tripMemberRepository.findTripMemberByTripPlanId(request.getCategoryDTOList().get(0).getTripPlanId());


        // 카테고리 전체 순환
        for (CategoryDTO categoryDTO : request.getCategoryDTOList()) {

            // 여행 멤버 전체 순환
            for (TripMember tripMember : tripMemberList) {

                Category category = categoryRepository.findByCategoryNameAndMemberIdNative(categoryDTO.getCategoryName(), tripMember.getTripMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다!"));

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

    /**
     * 저축 팁 관련 Prompt 및 저축 Tip 생성
     */
    @Transactional
    public void addSavingsTip(Long userId, Long planId) {
        // 1. 현재 저축 금액 조회
        int currentSavings = accountService.getUserBalance(userId);

        // 2. 여행 플랜 예산 조회 (목표 저축 금액)
        TripPlan tripPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("해당 플랜이 존재하지 않습니다."));
        int tripBudget = tripPlan.getTotalBudget();

        // 3. 최근 6개월 소비 내역 요약
        String baseYearMonth = YearMonth.now().toString(); // 예: "2025-09"
        List<MonthlySummaryDTO> summaries = transactionSummaryService.getMonthlySummary(userId, baseYearMonth);

        // 카테고리별 합산 (Map<Category, TotalAmount>)
        Map<String, Integer> categoryTotals = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (MonthlySummaryDTO monthSummary : summaries) {
            for (MonthlySummaryDTO.CategorySummaryDTO cat : monthSummary.getCategories()) {
                categoryTotals.merge(cat.getCategory(), cat.getTotalAmount(), Integer::sum);
                categoryCounts.merge(cat.getCategory(), cat.getTransactionCount(), Integer::sum);
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

        // 4. 프롬프트 작성
        String promptTemplate = PromptLoader.load("/prompts/savings-tip.txt");
        String promptText = String.format(
                promptTemplate,
                currentSavings,   // 현재 저축 금액
                tripBudget,       // 목표 저축 금액
                transactionSummary // 6개월 평균 소비 내역
        );

        // 5. GPT 호출
        SavingsTipResponseDTO response = chatClient
                .prompt()
                .system("너는 저축 조언 전문가야. 사용자의 소비 내역을 분석해서 맞춤형 저축 팁을 알려줘. \\\n" +
                        "반드시 예시를 참고하여 구어체를 사용하여 답변해.")
                .user(promptText)
                .call()
                .entity(SavingsTipResponseDTO.class);

        // 6. TripMember 조회 후 DB 저장
        TripMember tripMember = tripMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("TripMember가 없습니다."));

        for (String tip : response.getMessages()) {
            TripSavingPhrase phrase = TripSavingPhrase.builder()
                    .tripMember(tripMember)
                    .content(tip)
                    .build();
            tripSavingPhraseRepository.save(phrase);
        }
    }

    @Transactional
    public void checkSavingTip(Long userId, Long planId) {

        // 1. 플랜 참여 여부 확인
        boolean checkPlan = tripMemberRepository.existsByUser_UserId(userId);

        // 2. 계좌 확인
        boolean checkAccount = accountRepository.findByUserIdAndTripPlanId(userId, planId).isPresent();

        // 3. 카드 확인
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음!"));
        boolean checkCard = user.isCardConnected();

        // 4. 세 조건이 모두 true일 때만 실행
        if (checkPlan && checkAccount && checkCard) {
            addSavingsTip(userId, planId);
        }
    }
}

