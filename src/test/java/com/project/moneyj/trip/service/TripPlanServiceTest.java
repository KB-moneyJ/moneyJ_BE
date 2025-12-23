package com.project.moneyj.trip.service;

import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.trip.repository.*;
import com.project.moneyj.user.domain.Role;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripPlanServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TripPlanRepository tripPlanRepository;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private TripPlanService tripService;

    @Test
    @DisplayName("계좌가 3시간 이상 업데이트되지 않았다면 동기화 메서드(syncAccountIfNeeded)를 호출한다")
    void getUserBalances_should_call_sync_when_account_is_stale() {

        Instant fixedNow = Instant.parse("2025-01-01T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneId.of("UTC"));


        ReflectionTestUtils.setField(tripService, "clock", fixedClock);

        Long tripPlanId = 1L;
        LocalDate tripStartDate = LocalDate.of(2026, 5, 10);
        LocalDate tripEndDate = LocalDate.of(2026, 5, 14);
        LocalDate mockStartDate = LocalDate.of(2025, 12, 1);
        LocalDate mockTargetDate = LocalDate.of(2026, 4, 30);

        TripPlan tripPlan = TripPlan.of(
                3, "France", "FR", "Paris", 5, 4,
                tripStartDate, tripEndDate, 3000000, mockStartDate, mockTargetDate
        );
        User user = User.of("mmm", "mmmmmm@MMMMMM", "FKEOPSKFDPOFKE", Role.ROLE_USER);

        Account staleAccount = Account.of(user, tripPlan, "1234", "mask", 1000, "004", "name");
        Instant fourHoursAgo = fixedNow.minus(Duration.ofHours(4));
        ReflectionTestUtils.setField(staleAccount, "updatedAt", fourHoursAgo);

        given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
        given(accountRepository.findByTripPlanId(tripPlanId)).willReturn(List.of(staleAccount));
        given(categoryRepository.findByTripPlanId(tripPlanId)).willReturn(Collections.emptyList());

        tripService.getUserBalances(tripPlanId);

        verify(accountService, times(1)).syncAccountIfNeeded(staleAccount);
    }

    @Test
    @DisplayName("계좌가 3시간 이내라면 동기화 메서드 호출 안함")
    void getUserBalances_should_NOT_call_sync_when_account_is_fresh() {

        Instant fixedNow = Instant.parse("2025-01-01T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

        ReflectionTestUtils.setField(tripService, "clock", fixedClock);

        Long tripPlanId = 1L;
        LocalDate tripStartDate = LocalDate.of(2026, 5, 10);
        LocalDate tripEndDate = LocalDate.of(2026, 5, 14);
        LocalDate mockStartDate = LocalDate.of(2025, 12, 1);
        LocalDate mockTargetDate = LocalDate.of(2026, 4, 30);

        TripPlan tripPlan = TripPlan.of(
                3, "France", "FR", "Paris", 5, 4,
                tripStartDate, tripEndDate, 3000000, mockStartDate, mockTargetDate
        );
        User user = User.of("mmm", "mmmmmm@MMMMMM", "FKEOPSKFDPOFKE", Role.ROLE_USER);

        Account freshAccount = Account.of(user, tripPlan, "1234", "mask", 1000, "004", "name");
        Instant oneHourAgo = fixedNow.minus(Duration.ofHours(1));
        ReflectionTestUtils.setField(freshAccount, "updatedAt", oneHourAgo);

        given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
        given(accountRepository.findByTripPlanId(tripPlanId)).willReturn(List.of(freshAccount));
        given(categoryRepository.findByTripPlanId(tripPlanId)).willReturn(Collections.emptyList());

        tripService.getUserBalances(tripPlanId);

        verify(accountService, times(0)).syncAccountIfNeeded(any());
    }
}