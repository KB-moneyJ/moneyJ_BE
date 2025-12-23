package com.project.moneyj.trip.repository;

import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.trip.dto.TripPlanListDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    // 사용자별 모든 여행 플랜 조회
    @Query("""
        select tp
        from TripPlan tp
        left join fetch tp.tripMemberList tm
        where tm.user.userId = :userId
        """)
    List<TripPlan> findAllByUserId(@Param("userId") Long userId);


    // TODO: QueryDSL 사용 고려
    @Query("""
            SELECT new com.project.moneyj.trip.dto.TripPlanListDTO(
                tp.tripPlanId,
                tp.country,
                tp.countryCode,
                tp.city,
                tp.tripStartDate,
                tp.tripEndDate,
                tp.totalBudget,
                tp.membersCount,
                COALESCE((
                    SELECT SUM(a.balance)
                    FROM Account a
                    WHERE a.tripPlan.tripPlanId = tp.tripPlanId
                ), 0)
                +
                COALESCE((
                    SELECT SUM(c.amount)
                    FROM Category c
                    WHERE c.tripPlan.tripPlanId = tp.tripPlanId
                      AND c.isConsumed = TRUE
                ), 0)
            )
            FROM TripPlan tp
            WHERE EXISTS (
                SELECT 1
                FROM TripMember tm
                WHERE tm.tripPlan = tp
                  AND tm.user.userId = :userId
            )
      """)
    List<TripPlanListDTO> findAllWithProgress(@Param("userId") Long userId);

    // 여행 플랜 상세 조회
    @Query("""
        select tp
        from TripPlan tp
        left join fetch tp.tripMemberList tm
        left join fetch tm.user u
        where tp.tripPlanId = :planId
        """)
    Optional<TripPlan> findDetailById(@Param("planId") Long planId);

    TripPlan findByTripPlanId(Long tripPlanId);
}
