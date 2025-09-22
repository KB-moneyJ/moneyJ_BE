package com.project.moneyj.transaction.service.event;


import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.codef.service.CodefCardService;
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
        Map<String, Object> response = codefCardService.getCardApprovalList(user.getUserId(), event.getRequest());

        // data 안전하게 처리 (리스트/단일 객체 모두 대응)
        Object rawData = response.get("data");
        List<Map<String, Object>> data;
        if (rawData instanceof List<?> list) {
            data = (List<Map<String, Object>>) list;
        } else if (rawData instanceof Map<?, ?> map) {
            data = List.of((Map<String, Object>) map); // 단일 객체를 리스트로 감싸기
        } else {
            data = List.of(); // 비어있거나 예상치 못한 타입 처리
        }

        // 거래 DB 저장 (트랜잭션)
        transactionService.saveTransactions(user, data);

        // 요약 DB 저장 (트랜잭션)
        transactionSummaryService.initialize6MonthsSummary(user.getUserId());
    }
}
