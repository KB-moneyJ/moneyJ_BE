package com.project.moneyj.trip.repository;

import com.project.moneyj.trip.domain.TripMember;
import com.project.moneyj.trip.domain.TripPlan;
import com.project.moneyj.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    @Query("SELECT tm FROM TripMember tm WHERE tm.tripPlan.tripPlanId = :planId")
    List<TripMember> findTripMemberByTripPlanId(@Param("planId") Long planId);

    // 여행 플랜 삭제
    Optional<TripMember> findByTripPlanAndUser(TripPlan tripPlan, User user);


    @Query("SELECT tm FROM TripMember tm WHERE tm.user.userId = :userId and tm.tripPlan.tripPlanId = :planId")
    Optional<TripMember> findByUserIdAndPlanId(@Param("userId") Long userId, @Param("planId") Long planId);

    @Query("""
    select (count(tm) > 0)
    from TripMember tm
    where tm.user.userId = :userId
      and tm.tripPlan.tripPlanId = :planId
    """)
    boolean existsMemberByUserAndPlan(@Param("userId") Long userId, @Param("planId") Long planId);


    //트랜잭션 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select tm
    from TripMember tm
    where tm.user.userId = :userId
      and tm.tripPlan.tripPlanId = :planId
    """)
    TripMember findMemberForUpdate(Long userId, Long planId);

}
