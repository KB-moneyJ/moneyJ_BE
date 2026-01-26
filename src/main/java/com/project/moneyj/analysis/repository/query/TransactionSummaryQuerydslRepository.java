package com.project.moneyj.analysis.repository.query;

import static com.project.moneyj.analysis.domain.QTransactionSummary.transactionSummary;

import com.project.moneyj.analysis.dto.QSummaryQueryDTO;
import com.project.moneyj.analysis.dto.SummaryQueryDTO;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionSummaryQuerydslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<SummaryQueryDTO> findByUserIdBetweenMonths(Long userId, String from, String to) {
        return jpaQueryFactory
            .select(new QSummaryQueryDTO(
                transactionSummary.summaryMonth,
                transactionSummary.transactionCategory,
                transactionSummary.totalAmount,
                transactionSummary.transactionCount
            ))
            .from(transactionSummary)
            .where(
                transactionSummary.user.userId.eq(userId),
                transactionSummary.summaryMonth.between(from, to)
            )
            .orderBy(
                transactionSummary.summaryMonth.asc(),
                transactionSummary.totalAmount.desc()
            )
            .fetch();
    }

}
