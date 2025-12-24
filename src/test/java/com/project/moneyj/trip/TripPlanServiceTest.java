package com.project.moneyj.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.spy;

import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.trip.dto.TripPlanResponseDTO;
import com.project.moneyj.trip.repository.TripMemberRepository;
import com.project.moneyj.trip.repository.TripPlanRepository;
import com.project.moneyj.trip.service.TripPlanService;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class TripPlanServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripPlanRepository tripPlanRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TripPlanService tripPlanService;

    @Test
    @DisplayName("플랜 탈퇴 - 마지막 멤버 아닌 경우")
    void leavePlanTest_notLastMember() {
        // given
        Long planId = 1L;
        Long userId = 1L;

        User user = mock(User.class);
        TripPlan tripPlan = spy(TripPlan.class);
        ReflectionTestUtils.setField(tripPlan, "tripPlanId", 1L);
        ReflectionTestUtils.setField(tripPlan, "tripMemberList", new ArrayList<>());
        TripMember member = mock(TripMember.class);

        // 멤버가 2명
        tripPlan.getTripMemberList().add(member);
        tripPlan.getTripMemberList().add(mock(TripMember.class));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripPlanRepository.findByIdWithPessimisticLock(planId)).willReturn(Optional.of(tripPlan));
        given(tripMemberRepository.findByTripPlanAndUser(tripPlan, user)).willReturn(Optional.of(member));

        // when
        TripPlanResponseDTO response = tripPlanService.leavePlan(planId, userId);

        // then
        assertThat(response.getMessage()).isEqualTo("해당 플랜에서 탈퇴했습니다.");
        verify(accountRepository).deleteByTripPlanAndUser(tripPlan, user);
        assertThat(tripPlan.getTripMemberList()).hasSize(1);
        verify(tripPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("플랜 탈퇴 - 마지막 멤버인 경우 플랜 삭제")
    void leavePlanTest_lastMember() {
        // given
        Long planId = 1L;
        Long userId = 1L;

        User user = mock(User.class);
        TripPlan tripPlan = spy(TripPlan.class);
        ReflectionTestUtils.setField(tripPlan, "tripPlanId", 1L);
        ReflectionTestUtils.setField(tripPlan, "tripMemberList", new ArrayList<>());
        TripMember member = mock(TripMember.class);

        // 멤버가 1명
        tripPlan.getTripMemberList().add(member);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripPlanRepository.findByIdWithPessimisticLock(planId)).willReturn(Optional.of(tripPlan));
        given(tripMemberRepository.findByTripPlanAndUser(tripPlan, user)).willReturn(Optional.of(member));

        // when
        TripPlanResponseDTO response = tripPlanService.leavePlan(planId, userId);

        // then
        assertThat(response.getMessage()).isEqualTo("마지막 멤버가 탈퇴하여 플랜이 삭제되었습니다.");
        verify(tripPlanRepository).delete(tripPlan);
    }
}