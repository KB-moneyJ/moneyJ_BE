package com.project.moneyj.trip.tip.service;


import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.repository.TripMemberRepository;
import com.project.moneyj.trip.tip.repository.TripSavingPhraseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SavingTipBatchService {

    private final TripMemberRepository tripMemberRepository;
    private final TripSavingPhraseRepository tripSavingPhraseRepository;
    private final SavingTipService savingTipService;

    public void updateAllMemberSavingTip(){
        List<TripMember> members = tripMemberRepository.findAll();

        for(TripMember member : members){
            Long userId = member.getUser().getUserId();
            Long planId = member.getTripPlan().getTripPlanId();

            tripSavingPhraseRepository.deleteByTripMember_TripMemberId(member.getTripMemberId());

            savingTipService.updateSavingsTip(userId, planId);
        }

    }
}
