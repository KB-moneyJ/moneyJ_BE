package com.project.moneyj.trip.service;

import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.repository.TripMemberRepository;
import com.project.moneyj.trip.repository.TripSavingPhraseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingTipBatchService {

    private final TripMemberRepository tripMemberRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;
    private final TripPlanService tripPlanService;

    public void updateAllMemberSavingTip(){
        List<TripMember> members = tripMemberRepository.findAll();

        for(TripMember member : members){
            Long memberId = member.getUser().getUserId();
            Long planId = member.getTripPlan().getTripPlanId();

            tripSavingPhraseRepository.deleteByTripMember_TripMemberId(member.getTripMemberId());

            tripPlanService.updateSavingsTip(memberId, planId);
        }

    }
}
