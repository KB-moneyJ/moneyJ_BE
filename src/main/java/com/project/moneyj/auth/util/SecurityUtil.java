package com.project.moneyj.auth.util;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor
public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // 혹은 예외를 던지는 등 비인증 상태 처리
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }

        // CustomOAuth2User 타입이 아닌 경우 (예: 익명 사용자 등)
        // 이 로직에서는 발생하면 안되지만, 방어적 코드로 null 반환
        return null;
    }

}
