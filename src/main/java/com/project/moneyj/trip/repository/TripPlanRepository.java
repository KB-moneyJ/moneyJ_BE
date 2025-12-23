package com.project.moneyj.trip.repository;

import com.project.moneyj.trip.domain.TripPlan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 쓰기 잠금 걸어서 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tp from TripPlan tp where tp.tripPlanId = :planId")
    Optional<TripPlan> findByIdWithPessimisticLock(@Param("planId") Long planId);

}
