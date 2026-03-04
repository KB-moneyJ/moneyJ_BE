package com.project.moneyj.trip.plan.repository.query;

import static com.project.moneyj.account.domain.QAccount.account;
import static com.project.moneyj.trip.member.domain.QCategory.category;
import static com.project.moneyj.trip.member.domain.QTripMember.tripMember;
import static com.project.moneyj.trip.plan.domain.QTripPlan.tripPlan;
import static com.querydsl.core.types.dsl.Expressions.asNumber;

import com.project.moneyj.trip.plan.dto.QTripPlanQueryDTO;
import com.project.moneyj.trip.plan.dto.TripPlanQueryDTO;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TripPlanQuerydslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<TripPlanQueryDTO> findAllWithProgress(Long userId) {

        NumberExpression<Long> accountSum = asNumber(
            JPAExpressions.select(account.balance.sum().longValue().coalesce(0L))
                .from(account)
                .where(account.tripPlan.eq(tripPlan))
        );

        NumberExpression<Long> categorySum = asNumber(
            JPAExpressions.select(category.amount.sum().longValue().coalesce(0L))
                .from(category)
                .where(category.tripPlan.eq(tripPlan), category.isConsumed.isTrue())
        );

        return jpaQueryFactory
            .select(new QTripPlanQueryDTO(
                tripPlan.tripPlanId,
                tripPlan.country,
                tripPlan.countryCode,
                tripPlan.city,
                tripPlan.tripStartDate,
                tripPlan.tripEndDate,
                tripPlan.totalBudget,
                tripPlan.membersCount,
                accountSum,
                categorySum
            ))
            .from(tripPlan)
            .where(
                JPAExpressions
                    .selectOne()
                    .from(tripMember)
                    .where(tripMember.tripPlan.eq(tripPlan),
                        tripMember.user.userId.eq(userId))
                    .exists()
            )
            .fetch();
    }
}
