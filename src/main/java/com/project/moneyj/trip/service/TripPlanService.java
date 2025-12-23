package com.project.moneyj.trip.service;


import com.project.moneyj.account.Service.AccountService;
import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.codef.service.CodefBankService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CategoryErrorCode;
import com.project.moneyj.exception.code.TripMemberErrorCode;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.openai.util.PromptLoader;
import com.project.moneyj.trip.domain.Category;
import com.project.moneyj.trip.domain.MemberRole;
import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.trip.domain.TripSavingPhrase;
import com.project.moneyj.trip.dto.AddTripMemberRequestDTO;
import com.project.moneyj.trip.dto.CategoryDTO;
import com.project.moneyj.trip.dto.CategoryListRequestDTO;
import com.project.moneyj.trip.dto.CategoryResponseDTO;
import com.project.moneyj.trip.dto.SavingsTipResponseDTO;
import com.project.moneyj.trip.dto.TripBudgetRequestDTO;
import com.project.moneyj.trip.dto.TripBudgetResponseDTO;
import com.project.moneyj.trip.dto.TripPlanDetailResponseDTO;
import com.project.moneyj.trip.dto.TripPlanListResponseDTO;
import com.project.moneyj.trip.dto.TripPlanPatchRequestDTO;
import com.project.moneyj.trip.dto.TripPlanRequestDTO;
import com.project.moneyj.trip.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.dto.isConsumedRequestDTO;
import com.project.moneyj.trip.dto.isConsumedResponseDTO;
import com.project.moneyj.trip.repository.CategoryRepository;
import com.project.moneyj.trip.repository.TripMemberRepository;
import com.project.moneyj.trip.repository.TripPlanRepository;
import com.project.moneyj.trip.repository.TripSavingPhraseRepository;
import com.project.moneyj.trip.repository.TripTipRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Duration STALE_THRESHOLD = Duration.ofHours(3);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final TripPlanRepository tripPlanRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripTipRepository tripTipRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;

    private final ChatClient chatClient;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionSummaryService transactionSummaryService;
    private final CodefBankService codefBankService;

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
    public TripPlanResponseDTO patchPlan(Long planId, TripPlanPatchRequestDTO requestDTO) {

        TripPlan existingPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

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
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));


        // 여행 플랜 카테고리
        TripMember tripMember = tripMemberRepository.findTripMemberByTripPlanId(planId).get(0);

        List<Category> categoryList = categoryRepository.findByTripPlanIdAndTripMemberId(planId, tripMember.getTripMemberId());

        // 사용자 조회
        for (String email : addDTO.getEmail()) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

            if (tripMemberRepository.findByTripPlanAndUser(existingPlan, user).isPresent()) {
                throw MoneyjException.of(TripMemberErrorCode.ALREADY_EXISTS);
            }


            TripMember addTripMember = TripMember.of(user, null, MemberRole.MEMBER);

            addTripMember.addTripMember(existingPlan);

            for (Category category : categoryList) {
                Category newCategory = Category.of(
                                category.getCategoryName(),
                                category.getAmount(),
                                false,
                                existingPlan,
                                addTripMember);

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
     * 여행 멤버별 저축 금액 조회 및 업데이트
     * 마지막 동기화 < 3시간 -> DB에서 바로 금액 반환
     * 마지막 동기화 >= 3시간: CODEF 비동기(syncAccountIfNeeded) 호출
     */
    @Transactional(readOnly = true)
    public UserBalanceResponseDTO getUserBalances(Long tripPlanId) {

        // 요청된 플랜이 실제 존재하는지 확인
        TripPlan tripPlan = tripPlanRepository.findById(tripPlanId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        // 모든 여행 멤버들의 계좌 목록 조회
        List<Account> accounts = accountRepository.findByTripPlanId(tripPlanId);

        if (accounts.isEmpty()) {
            // 계좌가 하나도 없으면 팀 달성률 0, 리스트 빈값으로 반환
            return UserBalanceResponseDTO.builder()
                    .tripPlanProgress(0.0)
                    .userBalanceInfoList(List.of())
                    .build();
        }

        List<Category> categories = categoryRepository.findByTripPlanId(tripPlanId);

        Map<Long, List<Category>> categoriesMap = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getTripMember().getUser().getUserId()));

        // 각 개인별 달성률 계산
        List<UserBalanceResponseDTO.UserBalanceInfo> userBalanceInfos = accounts.stream()
                .map(account -> {

                    // 3시간 갱신 검사
                    if (account.isStale(STALE_THRESHOLD)) {
                        accountService.syncAccountIfNeeded(account);
                    }

                    Long currentUserId = account.getUser().getUserId();
                    List<Category> myCategories = categoriesMap.getOrDefault(currentUserId, Collections.emptyList());

                    int accountBalance = Optional.ofNullable(account.getBalance()).orElse(0);

                    int consumedCategorySum = myCategories.stream()
                            .filter(Category::isConsumed)
                            .mapToInt(c -> Optional.ofNullable(c.getAmount()).orElse(0))
                            .sum();

                    // 총 합
                    int effectiveBalance = accountBalance + consumedCategorySum;

                    double rawProgress = 0.0;
                    Integer totalBudget = tripPlan.getTotalBudget();

                    if (totalBudget != null && totalBudget > 0) {
                        rawProgress = (effectiveBalance * 100.0) / totalBudget;
                    }

                    double progress = BigDecimal.valueOf(rawProgress)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

                    return UserBalanceResponseDTO.UserBalanceInfo.builder()
                            .accountId(account.getAccountId())
                            .userId(account.getUser().getUserId())
                            .nickname(account.getUser().getNickname())
                            .profileImage(account.getUser().getProfileImage())
                            .balance(accountBalance)
                            .progress(progress)
                            .build();
                })
                .toList();

        // 여행 플랜 전체 달성률 계산
        double avgProgress = userBalanceInfos.stream()
                .mapToDouble(UserBalanceResponseDTO.UserBalanceInfo::getProgress)
                .average()
                .orElse(0.0);

        double tripPlanProgress = BigDecimal.valueOf(avgProgress)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return UserBalanceResponseDTO.builder()
                .tripPlanProgress(tripPlanProgress)
                .userBalanceInfoList(userBalanceInfos)
                .build();
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
                .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

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

    /**
     * 저축 팁 관련 Prompt 및 저축 Tip 생성
     */
    @Transactional
    public void addSavingsTip(Long userId, Long planId) {

        if (tripSavingPhraseRepository.existsByUserIdAndPlanId(userId, planId)) {
            return;
        }

        // 1. 현재 저축 금액 조회
        int currentSavings = accountService.getUserBalance(userId);

        // 2. 여행 플랜 예산 조회 (목표 저축 금액)
        TripPlan tripPlan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));
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
                .system("너는 저축 조언 전문가야. " +
                        "사용자의 소비 내역을 분석해서 반드시 3개의 맞춤형 저축 팁을 작성해줘. \\\n" +
                        "반드시 예시를 참고하여 구어체를 사용하여 답변해.")
                .user(promptText)
                .call()
                .entity(SavingsTipResponseDTO.class);

        // 6. TripMember 조회 후 DB 저장
        TripMember tripMember = tripMemberRepository.findByUserIdAndPlanId(userId, planId)
                .orElseThrow(() -> MoneyjException.of(TripMemberErrorCode.NOT_FOUND));

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
       if (!accountRepository.findByUserIdAndTripPlanId(userId, planId).isPresent()){
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
}

