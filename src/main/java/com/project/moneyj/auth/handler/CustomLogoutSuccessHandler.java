package com.project.moneyj.auth.handler;

import com.project.moneyj.auth.util.SecurityResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final SecurityResponseUtil securityResponseUtil;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication) throws IOException {
        Map<String, Object> body = Map.of(
            "status", "success",
            "message", "로그아웃 완료"
        );

        securityResponseUtil.writeSuccess(response, HttpStatus.OK, body);
    }
}
