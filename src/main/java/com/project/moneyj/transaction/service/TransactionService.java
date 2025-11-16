package com.project.moneyj.transaction.service;

import com.project.moneyj.transaction.domain.Transaction;
import com.project.moneyj.transaction.repository.TransactionRepository;
import com.project.moneyj.user.domain.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Transactional
    public void saveTransactions(User user, List<Map<String, Object>> data) {
        List<Transaction> transactions = data.stream()
            .map(raw -> toTransaction(raw, user))
            .toList();

        transactionRepository.saveAll(transactions);
    }

    public Transaction toTransaction(Map<String, Object> raw, User user) {
        String resUsedDate = (String) raw.get("resUsedDate");
        String resUsedTime = (String) raw.get("resUsedTime");

        LocalDateTime usedDateTime = LocalDateTime.parse(
            resUsedDate + resUsedTime,
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );

        // 실제 승인/취소 금액 계산
        String rawUsed = (String) raw.get("resUsedAmount");
        String rawCancelYN = (String) raw.get("resCancelYN");
        String rawCancelAmount = (String) raw.get("resCancelAmount");

        int usedAmount = safeParseInt(rawUsed);
        int cancelAmount = safeParseInt(rawCancelAmount);

        int actualAmount = "0".equals(rawCancelYN)
            ? usedAmount
            : (cancelAmount > 0 ? usedAmount - cancelAmount : usedAmount);

        return Transaction.of(user,
                        StoreCategoryMapper.mapToCategory((String) raw.get("resMemberStoreType")),
                        usedDateTime,
                        actualAmount,
                        (String) raw.get("resMemberStoreName"),
                        (String) raw.get("resMemberStoreCorpNo"),
                        (String) raw.get("resMemberStoreAddr"),
                        (String) raw.get("resMemberStoreNo"),
                        (String) raw.get("resMemberStoreType"),
                        (String) raw.get("resApprovalNo"),
                        LocalDateTime.now());
    }

    private int safeParseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}