package com.project.moneyj.transaction.service;

import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionFacade {
    private final UserRepository userRepository;
    private final CodefCardService codefCardService;
    private final TransactionService transactionService;

    public void processTransactions(Long userId, CardApprovalRequestDTO req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!user.isCardConnected()) {
            user.connectCard();
        }

        // 외부 api 호출 (트랜잭션 x)
        Map<String, Object> response = codefCardService.getCardApprovalList(userId, req);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        // DB 처리 (트랜잭션 o)
        transactionService.saveTransactions(user, data);
    }
}
