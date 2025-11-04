package com.project.moneyj.auth.service;

import com.project.moneyj.auth.domain.TempAuthCode;
import com.project.moneyj.auth.dto.TokenResponse;
import com.project.moneyj.auth.repository.TempAuthCodeRepository;
import com.project.moneyj.auth.util.JwtUtil;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.AuthErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempAuthCodeService {

    private final TempAuthCodeRepository tempAuthCodeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public TokenResponse exchangeTempCode(String code) {
        TempAuthCode tempAuthCode = tempAuthCodeRepository.findById(code)
            .orElseThrow(() -> MoneyjException.of(AuthErrorCode.INVALID_TEMP_AUTH_CODE));

        if (tempAuthCode.isExpired()) {
            tempAuthCodeRepository.delete(tempAuthCode);
            throw MoneyjException.of(AuthErrorCode.EXPIRED_TEMP_AUTH_CODE);
        }

        User user = userRepository.findById(tempAuthCode.getUserId())
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        String jwt = jwtUtil.generateToken(user.getUserId().toString());
        tempAuthCodeRepository.delete(tempAuthCode);

        return new TokenResponse(jwt, tempAuthCode.isFirstLogin());
    }
}
