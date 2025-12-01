package com.project.moneyj.auth.handler;

import com.project.moneyj.auth.util.SecurityResponseUtil;
import com.project.moneyj.exception.code.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@AllArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityResponseUtil securityResponseUtil;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        securityResponseUtil.writeError(request, response, AuthErrorCode.JWT_TOKEN_IS_EMPTY);
    }

}
