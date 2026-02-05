package com.project.moneyj.trip.tip.service;


import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.member.service.TripMemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SavingTipBatchService {

    private final TripMemberService tripMemberService;
    private final SavingTipService savingTipService;

    public void updateAllMemberSavingTip(){
        List<TripMember> members = tripMemberService.getTripMembers();

        for(TripMember member : members){
            Long userId = member.getUser().getUserId();
            Long planId = member.getTripPlan().getTripPlanId();
            Long memberId = member.getTripMemberId();
            savingTipService.updateSavingsTip(userId, planId, memberId);
        }
    }
}
