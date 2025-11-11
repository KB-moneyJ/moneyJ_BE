package com.project.moneyj.auth.handler;

import com.project.moneyj.auth.util.SecurityResponseUtil;
import com.project.moneyj.exception.code.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityResponseUtil securityResponseUtil;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        securityResponseUtil.writeError(request, response, AuthErrorCode.OAUTH2_LOGIN_FAILED);
    }

}
