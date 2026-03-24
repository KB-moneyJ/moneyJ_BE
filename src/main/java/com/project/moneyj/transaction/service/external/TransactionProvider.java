package com.project.moneyj.transaction.service.external;

import com.project.moneyj.transaction.dto.ExternalTransactionDTO;
import com.project.moneyj.transaction.dto.TransactionRequestDTO;

import java.util.List;

public interface TransactionProvider {
    List<ExternalTransactionDTO> fetchTransactions(Long userId, TransactionRequestDTO request);
}