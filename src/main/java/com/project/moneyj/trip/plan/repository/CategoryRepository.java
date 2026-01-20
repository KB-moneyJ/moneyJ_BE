package com.project.moneyj.trip.plan.repository;

import com.project.moneyj.trip.plan.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
      SELECT c
      FROM Category c
      WHERE c.tripPlan.tripPlanId = :tripPlanId
        AND c.tripMember.tripMemberId = :tripMemberId
    """)
    List<Category> findByTripPlanIdAndTripMemberId(@Param("tripPlanId") Long tripPlanId, @Param("tripMemberId") Long tripMemberId);

    @Query("""
        SELECT c
        FROM Category c
        JOIN FETCH c.tripMember tm
        JOIN FETCH tm.user
        WHERE c.tripPlan.tripPlanId = :tripPlanId
    """)
    List<Category> findByTripPlanId(@Param("tripPlanId") Long tripPlanId);

    @Query("SELECT c FROM Category c WHERE c.categoryName = :categoryName AND c.tripMember.tripMemberId = :memberId")
    Optional<Category> findByCategoryNameAndMemberIdNative(@Param("categoryName") String categoryName, @Param("memberId") Long memberId);

}
