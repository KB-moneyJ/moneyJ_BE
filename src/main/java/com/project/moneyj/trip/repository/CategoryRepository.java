package com.project.moneyj.trip.repository;

import com.project.moneyj.trip.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(value = "SELECT * FROM category WHERE trip_plan_id = :tripPlanId AND trip_member_id = :tripMemberId", nativeQuery = true)
    List<Category> findByTripPlanIdAndTripMemberId(@Param("tripPlanId") Long tripPlanId, @Param("tripMemberId") Long tripMemberId);

    @Query(value = "SELECT * FROM category WHERE trip_plan_id = :tripPlanId", nativeQuery = true)
    List<Category> findByTripPlanId(Long tripPlanId);

    @Query(value = "SELECT * FROM category WHERE category_name = :categoryName AND trip_member_id = :memberId", nativeQuery = true)
    Optional<Category> findByCategoryNameAndMemberIdNative(@Param("categoryName") String categoryName, @Param("memberId") Long memberId);

}
