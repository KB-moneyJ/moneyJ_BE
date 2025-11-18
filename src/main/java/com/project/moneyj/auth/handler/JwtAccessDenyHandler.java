package com.project.moneyj.auth.handler;

import com.project.moneyj.auth.util.SecurityResponseUtil;
import com.project.moneyj.exception.code.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@AllArgsConstructor
public class JwtAccessDenyHandler implements AccessDeniedHandler {

    private final SecurityResponseUtil securityResponseUtil;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        securityResponseUtil.writeError(request, response, AuthErrorCode.FORBIDDEN_CLIENT);
    }
}
