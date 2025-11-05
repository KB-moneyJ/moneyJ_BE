package com.project.moneyj.transaction.service;

import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.transaction.domain.event.TransactionRequestEvent;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionFacade {
    private final UserRepository userRepository;
    private final CodefCardService codefCardService;
    private final TransactionService transactionService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void processTransactions(Long userId, CardApprovalRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        if (!user.isCardConnected()) {
            user.connectCard();
        }

        // 이벤트 발행으로 외부 api 호출 및 DB 저장 처리 넘김
        applicationEventPublisher.publishEvent(new TransactionRequestEvent(userId, req));
    }
}
