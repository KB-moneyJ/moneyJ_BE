package com.project.moneyj.analysis.dto;

import com.project.moneyj.transaction.domain.TransactionCategory;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class SummaryQueryDTO {
    private String summaryMonth;
    private TransactionCategory category;
    private Integer totalAmount;
    private Integer transactionCount;

    @QueryProjection
    public SummaryQueryDTO(
        String summaryMonth,
        TransactionCategory category,
        Integer totalAmount,
        Integer transactionCount
    ) {
        this.summaryMonth = summaryMonth;
        this.category = category;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }
}
