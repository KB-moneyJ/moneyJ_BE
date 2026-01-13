package com.project.moneyj.analysis.repository;

import com.project.moneyj.analysis.domain.TransactionSummary;
import com.project.moneyj.transaction.domain.TransactionCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionSummaryRepository extends JpaRepository<TransactionSummary, Long> {

    // 특정 유저의 특정 월 TransactionSummary 조회
    List<TransactionSummary> findByUser_UserIdAndSummaryMonth(Long userId, String summaryMonth);

    // 특정 유저의 특정 월의 특정 카테고리 TransactionSummary 조회
    Optional<TransactionSummary> findByUser_UserIdAndSummaryMonthAndTransactionCategory(
        Long userId,
        String month,
        TransactionCategory category
    );
}
