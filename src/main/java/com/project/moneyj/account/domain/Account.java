package com.project.moneyj.account.domain;

import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_number",
                        columnNames = {"account_number"}
                ),

                @UniqueConstraint(
                        name = "uk_account_plan_user",
                        columnNames = {"trip_plan_id", "user_id"}
                )
        }
)
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

    @Column(name = "account_number")
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

