package com.project.moneyj.transaction.service;

import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionSyncScheduler {
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    /**
     * 매주 월요일 00시에 카드 소비내역 갱신
     */
    @Scheduled(cron = "0 0 0 ? * MON" , zone = "Asia/Seoul")
    public void weeklySync() {

        LocalDate today = LocalDate.now();

        String endDate   = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String startDate = today.minusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);

        List<User> users = userRepository.findAllByCardConnectedTrue();

        for (User user : users) {

            List<Card> cards = cardRepository.findAllByUser(user);

            if (cards.isEmpty()) continue;

            for (Card card : cards) {

                String organization = card.getOrganizationCode();

                CardApprovalRequestDTO req = CardApprovalRequestDTO.builder()
                        .organization(organization)
                        .startDate(startDate)
                        .endDate(endDate)
                        .orderBy("0")
                        .inquiryType("1")
                        .build();

                transactionService.updateWeeklyTransactions(user, req);
            }
        }
    }
}
