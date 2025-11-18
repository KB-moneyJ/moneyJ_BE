package com.project.moneyj.auth.repository;

import com.project.moneyj.auth.domain.TempAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempAuthCodeRepository extends JpaRepository<TempAuthCode, String> {

}
