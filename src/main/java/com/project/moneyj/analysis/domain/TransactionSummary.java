package com.project.moneyj.analysis.domain;

import com.project.moneyj.common.BaseTimeEntity;
import com.project.moneyj.transaction.domain.TransactionCategory;
import com.project.moneyj.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "transaction_summary",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "yearMonth", "transactionCategory"
    })
})
public class TransactionSummary extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transaction_summary_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private TransactionCategory transactionCategory;

    private String summaryMonth;
    private Integer totalAmount = 0;
    private Integer transactionCount = 0;
    //private LocalDate updateAt;

    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private TransactionSummary(Long transaction_summary_id,
                              User user,
                              TransactionCategory transactionCategory,
                              String summaryMonth,
                              Integer totalAmount,
                              Integer transactionCount,
                              LocalDate updateAt) {

        this.transaction_summary_id = transaction_summary_id;
        this.user = user;
        this.transactionCategory = transactionCategory;
        this.summaryMonth = summaryMonth;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    // === 정적 팩토리 메서드 ===
    public static TransactionSummary of(Long transaction_summary_id,
                                        User user,
                                        TransactionCategory transactionCategory,
                                        String summaryMonth,
                                        Integer totalAmount,
                                        Integer transactionCount,
                                        LocalDate updateAt) {

        return TransactionSummary.builder()
                .transaction_summary_id(transaction_summary_id)
                .user(user)
                .transactionCategory(transactionCategory)
                .summaryMonth(summaryMonth)
                .totalAmount(totalAmount)
                .transactionCount(transactionCount)
                .build();
    }
}
