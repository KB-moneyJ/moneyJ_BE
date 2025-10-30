package com.project.moneyj.trip.domain;

import com.project.moneyj.trip.dto.CategoryDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category")
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    private String categoryName;

    private Integer amount;

    @Builder.Default
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

    // 소비 상태만 변경
    public void changeConsumptionStatus(boolean consumed) {
        this.isConsumed = consumed;
    }
}