package com.project.moneyj.user.repository;

import com.project.moneyj.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(Long userId);

    List<User> findAllByEmailIn(List<String> emails);
    List<User> findAllByCardConnectedTrue();
}
