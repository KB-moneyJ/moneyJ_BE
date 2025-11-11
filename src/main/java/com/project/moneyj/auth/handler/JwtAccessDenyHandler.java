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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@AllArgsConstructor
public class JwtAccessDenyHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            AuthErrorCode.FORBIDDEN_CLIENT.httpStatus(),
            AuthErrorCode.FORBIDDEN_CLIENT.message()
        );
        problemDetail.setTitle(AuthErrorCode.FORBIDDEN_CLIENT.code());
        problemDetail.setInstance(URI.create(request.getRequestURI())); // 요청 url

        response.setStatus(AuthErrorCode.FORBIDDEN_CLIENT.httpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String jsonErrorResponse = objectMapper.writeValueAsString(problemDetail);
        response.getWriter().write(jsonErrorResponse);
    }
}
