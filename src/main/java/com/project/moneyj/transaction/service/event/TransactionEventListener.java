package com.project.moneyj.transaction.service.event;


import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.codef.dto.CodefCardApprovalDTO;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.transaction.domain.Transaction;
import com.project.moneyj.transaction.domain.event.TransactionRequestEvent;
import com.project.moneyj.transaction.service.TransactionService;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {
    private final UserRepository userRepository;
    private final CodefCardService codefCardService;
    private final TransactionService transactionService;
    private final TransactionSummaryService transactionSummaryService;

    @Async("transactionExecutor")
    @EventListener
    public void handleTransaction(TransactionRequestEvent event) {
        log.info("카드 내역 & 거래 저장 & 요약 저장 리스너 스레드: {}", Thread.currentThread().getName());
        User user = userRepository.findById(event.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 외부 API 호출
        List<CodefCardApprovalDTO> response = codefCardService.getCardApprovalList(user.getUserId(), event.getRequest());

        if (response == null || response.isEmpty()) {
            return; // 이번 주에 거래가 없다면 종료
        }

        // 거래 DB 저장 (트랜잭션)
        List<Transaction> transactions = transactionService.saveTransactions(user, response);

        // 요약 DB 저장 (트랜잭션)
        transactionSummaryService.initialize6MonthsSummary(user.getUserId());
    }
}
