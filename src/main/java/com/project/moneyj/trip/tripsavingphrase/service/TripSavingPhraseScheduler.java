package com.project.moneyj.trip.tripsavingphrase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripSavingPhraseScheduler {

    private final TripSavingPhraseBatchService tripSavingPhraseBatchService;

    /**
     * 매월 1일 00:00에 저축 팁 갱신
     */
    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Seoul")
    public void updateTripSavingPhrases(){
        log.info("저축 팁 갱신 스케쥴러 시작");
        tripSavingPhraseBatchService.updateAllMemberTripSavingPhrases();
        log.info("저축 팁 갱신 스케쥴러 종료");
    }

}
