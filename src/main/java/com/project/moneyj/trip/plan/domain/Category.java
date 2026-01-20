package com.project.moneyj.trip.plan.domain;

import com.project.moneyj.common.BaseTimeEntity;
import com.project.moneyj.trip.member.domain.TripMember;
import com.project.moneyj.trip.plan.dto.category.CategoryDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category")
public class Category extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    private String categoryName;

    private Integer amount;

    private boolean isConsumed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id")
    private TripPlan tripPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_member_id")
    private TripMember tripMember;

    // 소비 상태는 제외
    public void update(CategoryDTO dto) {

        if (dto.getCategoryName() != null) {
            this.categoryName = dto.getCategoryName();
        }
        if (dto.getAmount() != null) {
            this.amount = dto.getAmount();
        }
    }

    // === 생성자 (도메인 내부용) ===
    @Builder(access = AccessLevel.PRIVATE)
    private Category(String categoryName, Integer amount, boolean isConsumed, TripPlan tripPlan, TripMember tripMember) {
        this.categoryName = categoryName;
        this.amount = amount;
        this.isConsumed = isConsumed;
        this.tripPlan = tripPlan;
        this.tripMember = tripMember;
    }

    // === 정적 팩토리 메서드 ===
    public static Category of(String categoryName, Integer amount, boolean isConsumed, TripPlan tripPlan, TripMember tripMember){

        return Category.builder()
                .categoryName(categoryName)
                .amount(amount)
                .isConsumed(isConsumed)
                .tripPlan(tripPlan)
                .tripMember(tripMember)
                .build();
    }

    // 소비 상태만 변경
    public void changeConsumptionStatus(boolean consumed) {
        this.isConsumed = consumed;
    }
}