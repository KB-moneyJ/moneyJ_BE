package com.project.moneyj.trip.member.service;


import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.TripMemberErrorCode;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.trip.member.domain.MemberRole;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.dto.AddTripMemberRequestDTO;
import com.project.moneyj.trip.member.dto.UserBalanceResponseDTO;
import com.project.moneyj.trip.member.repository.TripMemberRepository;
import com.project.moneyj.trip.plan.domain.Category;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.dto.plan.TripPlanResponseDTO;
import com.project.moneyj.trip.plan.repository.CategoryRepository;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripMemberService {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(3);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final TripPlanRepository tripPlanRepository;
    private final TripMemberRepository tripMemberRepository;

    private final AccountRepository accountRepository;
    private final AccountService accountService;

    private final Clock clock;


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
     * 여행 멤버별 저축 금액 조회 및 업데이트
     * 마지막 동기화 < 3시간 -> DB에서 바로 금액 반환
     * 마지막 동기화 >= 3시간: CODEF (syncAccountIfNeeded) 호출
     */
    // TODO: 달성률 계산 부분 calcProgress 사용 고려
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
                    if (account.isStale(STALE_THRESHOLD, this.clock)) {
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

}

