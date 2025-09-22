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

        return Transaction.builder()
            .user(user)
            .usedDateTime(usedDateTime)
            .usedAmount(Integer.valueOf((String) raw.get("resUsedAmount")))
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