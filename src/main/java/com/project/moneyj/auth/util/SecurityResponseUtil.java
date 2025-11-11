package com.project.moneyj.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityResponseUtil {

    private final ObjectMapper objectMapper;

    public void writeError(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            errorCode.httpStatus(),
            errorCode.message()
        );
        problemDetail.setTitle(errorCode.code());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(errorCode.httpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

    public void writeSuccess(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
