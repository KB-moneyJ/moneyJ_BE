package com.project.moneyj.account.domain;

import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Data
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id")
    private TripPlan tripPlan;

    private String accountNumber;

    private String accountNumberMasked;

    private Integer balance;

    private String organizationCode;

    private String accountName;

    // === 기본 생성자 (내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private Account(User user,
                    TripPlan tripPlan,
                    String accountNumber,
                    String accountNumberMasked,
                    Integer balance,
                    String organizationCode,
                    String accountName) {

        this.user = user;
        this.tripPlan = tripPlan;
        this.accountNumber = accountNumber;
        this.accountNumberMasked = accountNumberMasked;
        this.balance = balance;
        this.organizationCode = organizationCode;
        this.accountName = accountName;
    }

    // === 정적 팩토리 메서드 ===
    public static Account of(User user,
                             TripPlan tripPlan,
                             String accountNumber,
                             String accountNumberMasked,
                             Integer balance,
                             String organizationCode,
                             String accountName) {

        return Account.builder()
                .user(user)
                .tripPlan(tripPlan)
                .accountNumber(accountNumber)
                .accountNumberMasked(accountNumberMasked)
                .balance(balance)
                .organizationCode(organizationCode)
                .accountName(accountName)
                .build();
    }

    // === 비즈니스 로직 ===
    public void updateBalance(Integer balance) {
        this.balance = balance;
    }

}

