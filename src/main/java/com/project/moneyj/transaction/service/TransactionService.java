package com.project.moneyj.transaction.service;

import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.transaction.domain.Transaction;
import com.project.moneyj.transaction.dto.ExternalTransactionDTO;
import com.project.moneyj.transaction.dto.TransactionRequestDTO;
import com.project.moneyj.transaction.repository.TransactionRepository;
import com.project.moneyj.transaction.service.external.TransactionProvider;
import com.project.moneyj.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionProvider transactionProvider;
    private final TransactionSummaryService transactionSummaryService;

    @Transactional
    public List<Transaction> saveTransactions(User user, List<ExternalTransactionDTO> data)
    {
        List<Transaction> transactions = data.stream()
                .map(dto -> toTransaction(dto, user))
                .toList();

        transactionRepository.saveAll(transactions);
        return transactions;
    }

    public Transaction toTransaction(ExternalTransactionDTO dto, User user) {
        return Transaction.of(
                user,
                StoreCategoryMapper.mapToCategory(dto.storeType()),
                dto.usedDateTime(),
                dto.actualAmount(),
                dto.storeName(),
                dto.storeCorpNo(),
                dto.storeAddr(),
                dto.storeNo(),
                dto.storeType(),
                dto.approvalNo(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public void updateWeeklyTransactions(User user, TransactionRequestDTO request) {

        List<ExternalTransactionDTO> response = transactionProvider.fetchTransactions(user.getUserId(), request);

        if (response == null || response.isEmpty()) {
            return; // 이번 주에 거래가 없다면 종료
        }

        List<Transaction> newTransactions = saveTransactions(user, response);

        transactionSummaryService.updateCurrentMonthSummary(user.getUserId(), newTransactions);
    }
}