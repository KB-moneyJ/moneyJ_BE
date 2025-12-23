package com.project.moneyj.transaction.domain;

import com.project.moneyj.common.BaseTimeEntity;
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
import java.time.LocalDateTime;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transaction")
public class Transaction extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transaction_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private TransactionCategory transactionCategory;

    private LocalDateTime usedDateTime; // resUsedDate + resUsedTime

    private Integer usedAmount;

    private String storeName;
    private String storeCorpNo;
    private String storeAddr;
    private String storeNo;
    private String storeType;
    private String approvalNo;


    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private Transaction(User user,
                        TransactionCategory transactionCategory,
                        LocalDateTime usedDateTime,
                        Integer usedAmount,
                        String storeName,
                        String storeCorpNo,
                        String storeAddr,
                        String storeNo,
                        String storeType,
                        String approvalNo,
                        LocalDateTime updateAt) {

        this.user = user;
        this.transactionCategory = transactionCategory;
        this.usedDateTime = usedDateTime;
        this.usedAmount = usedAmount;
        this.storeName = storeName;
        this.storeCorpNo = storeCorpNo;
        this.storeAddr = storeAddr;
        this.storeNo = storeNo;
        this.storeType = storeType;
        this.approvalNo = approvalNo;
    }

    // === 정적 팩토리 메서드 ===
    public static Transaction of(User user,
                                 TransactionCategory transactionCategory,
                                 LocalDateTime usedDateTime,
                                 Integer usedAmount,
                                 String storeName,
                                 String storeCorpNo,
                                 String storeAddr,
                                 String storeNo,
                                 String storeType,
                                 String approvalNo,
                                 LocalDateTime updateAt) {

        return Transaction.builder()
                .user(user)
                .transactionCategory(transactionCategory)
                .usedDateTime(usedDateTime)
                .usedAmount(usedAmount)
                .storeName(storeName)
                .storeCorpNo(storeCorpNo)
                .storeAddr(storeAddr)
                .storeNo(storeNo)
                .storeType(storeType)
                .approvalNo(approvalNo)
                .build();
    }

    // === 연관관계 메소드 ===
    public void addTransaction(User user){
        this.user = user;
        user.getTransactionList().add(this);
    }
}