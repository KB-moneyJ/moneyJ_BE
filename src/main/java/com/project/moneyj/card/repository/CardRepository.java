package com.project.moneyj.card.repository;

import com.project.moneyj.account.domain.Account;
import com.project.moneyj.card.domain.Card;
import com.project.moneyj.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByUser_UserId(Long userUserId);

    Optional<Card> findByUser_UserIdAndOrganizationCode(Long userId, String organization);

    // 특정 사용자가 특정 카드번호를 이미 등록했는지 확인
    Optional<Card> findByUserAndCardNo(User user, String cardNo);

    // 카드 번호로 카드 찾기
    Optional<Card> findByCardNo(String cardNo);

    List<Card> findAllByUser(User user);
}
