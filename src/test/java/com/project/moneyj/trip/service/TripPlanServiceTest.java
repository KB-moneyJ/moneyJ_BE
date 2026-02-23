package com.project.moneyj.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.spy;

import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.trip.plan.service.TripPlanService;
import java.util.ArrayList;
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
    private TripPlanRepository tripPlanRepository;

    @InjectMocks
    private TripPlanService tripPlanService;

    @Test
    @DisplayName("플랜 탈퇴 - 마지막 멤버 아닌 경우")
    void leavePlanTest_notLastMember() {
        // given
        TripPlan tripPlan = spy(TripPlan.class);
        ReflectionTestUtils.setField(tripPlan, "tripPlanId", 1L);
        ReflectionTestUtils.setField(tripPlan, "tripMemberList", new ArrayList<>());
        TripMember member = mock(TripMember.class);

        // 멤버가 2명
        tripPlan.getTripMemberList().add(member);
        tripPlan.getTripMemberList().add(mock(TripMember.class));

        // when
        tripPlanService.leaveTripPlan(tripPlan, member);

        // then
        assertThat(tripPlan.getTripMemberList()).hasSize(1);
        verify(tripPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("플랜 탈퇴 - 마지막 멤버인 경우 플랜 삭제")
    void leavePlanTest_lastMember() {
        // given
        TripPlan tripPlan = spy(TripPlan.class);
        ReflectionTestUtils.setField(tripPlan, "tripPlanId", 1L);
        ReflectionTestUtils.setField(tripPlan, "tripMemberList", new ArrayList<>());
        TripMember member = mock(TripMember.class);

        // 멤버가 1명
        tripPlan.getTripMemberList().add(member);

        // when
        tripPlanService.leaveTripPlan(tripPlan, member);

        // then
        assertThat(tripPlan.getTripMemberList()).isEmpty();
        verify(tripPlanRepository).delete(tripPlan);
    }
}