package com.project.moneyj.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.exception.code.AuthErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.AllArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@AllArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            AuthErrorCode.JWT_TOKEN_IS_EMPTY.httpStatus(),
            AuthErrorCode.JWT_TOKEN_IS_EMPTY.message()
        );
        problemDetail.setTitle(AuthErrorCode.JWT_TOKEN_IS_EMPTY.code());
        problemDetail.setInstance(URI.create(request.getRequestURI())); // 요청 url

        response.setStatus(AuthErrorCode.JWT_TOKEN_IS_EMPTY.httpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String jsonErrorResponse = objectMapper.writeValueAsString(problemDetail);
        response.getWriter().write(jsonErrorResponse);
    }

}
