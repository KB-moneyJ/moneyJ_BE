package com.project.moneyj.transaction.service;

import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.dto.CodefCardApprovalDTO;
import com.project.moneyj.codef.service.CodefCardService;
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
    private final CodefCardService codefCardService;
    private final TransactionSummaryService transactionSummaryService;

    @Transactional
    public List<Transaction> saveTransactions(User user, List<CodefCardApprovalDTO> data)
    {
        List<Transaction> transactions = data.stream()
                .map(dto -> toTransaction(dto, user))
                .toList();

        transactionRepository.saveAll(transactions);
        return transactions;
    }

    public Transaction toTransaction(CodefCardApprovalDTO dto, User user) {
        return Transaction.of(
                user,
                StoreCategoryMapper.mapToCategory(dto.resMemberStoreType()),
                dto.getUsedDateTime(),
                dto.getActualAmount(),
                dto.resMemberStoreName(),
                dto.resMemberStoreCorpNo(),
                dto.resMemberStoreAddr(),
                dto.resMemberStoreNo(),
                dto.resMemberStoreType(),
                dto.resApprovalNo(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public void updateWeeklyTransactions(User user, CardApprovalRequestDTO req) {

        List<CodefCardApprovalDTO> response = codefCardService.getCardApprovalList(user.getUserId(), req);

        if (response == null || response.isEmpty()) {
            return; // 이번 주에 거래가 없다면 종료
        }

        List<Transaction> newTransactions = saveTransactions(user, response);

        transactionSummaryService.updateCurrentMonthSummary(user.getUserId(), newTransactions);
    }
}