package com.project.moneyj.account.repository;

import com.project.moneyj.account.domain.Account;
import java.util.List;
import java.util.Optional;

import com.project.moneyj.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("""
      SELECT DISTINCT a
      FROM Account a
      JOIN FETCH a.user u
      WHERE a.tripPlan.tripPlanId = :tripPlanId
      ORDER BY a.balance DESC
    """)
    List<Account> findByTripPlanId(@Param("tripPlanId") Long tripPlanId);

    Optional<Account> findByUser_UserId(Long userUserId);

    Optional<Account> findByUser_UserIdAndOrganizationCode(Long userId, String organizationCode);

    @Query("SELECT a FROM Account a WHERE a.user.userId = :userId AND a.tripPlan.tripPlanId = :tripPlanId")
    Optional<Account> findByUserIdAndTripPlanId(@Param("userId") Long userId,
                                                @Param("tripPlanId") Long tripPlanId);

    Optional<Account> findByAccountNumber(String accountNumber);
}
