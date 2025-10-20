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
        int resUsedAmount = Integer.parseInt((String) raw.get("resUsedAmount"));
        String resCancelYN = (String) raw.get("resCancelYN");
        String resCancelAmount = (String) raw.get("resCancelAmount");
        int cancelAmount = (resCancelAmount == null || resCancelAmount.isEmpty()) ? 0 : Integer.parseInt(resCancelAmount);
        int actualAmount = "0".equals(resCancelYN)
            ? resUsedAmount
            : (cancelAmount > 0 ? resUsedAmount - cancelAmount : resUsedAmount);


        return Transaction.builder()
            .user(user)
            .usedDateTime(usedDateTime)
            .usedAmount(actualAmount)
            .storeName((String) raw.get("resMemberStoreName"))
            .storeCorpNo((String) raw.get("resMemberStoreCorpNo"))
            .storeAddr((String) raw.get("resMemberStoreAddr"))
            .storeNo((String) raw.get("resMemberStoreNo"))
            .storeType((String) raw.get("resMemberStoreType"))
            .approvalNo((String) raw.get("resApprovalNo"))
            .transactionCategory(StoreCategoryMapper.mapToCategory(
                (String) raw.get("resMemberStoreType")))
            .updateAt(LocalDateTime.now())
            .build();
    }
}