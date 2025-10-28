package com.project.moneyj.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class MoneyjExceptionHandler {

    @ExceptionHandler(MoneyjException.class)
    public ResponseEntity<ProblemDetail> handleMoneyjException(
        HttpServletRequest request,
        MoneyjException exception
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            exception.getErrorCode().httpStatus(),
            exception.getErrorCode().message()
        );
        problemDetail.setTitle(exception.getErrorCode().code());
        problemDetail.setInstance(URI.create(request.getRequestURI())); // 요청 url

        return new ResponseEntity<>(problemDetail, exception.getErrorCode().httpStatus()); // json + http 상태 코드
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(
        HttpServletRequest request,
        Exception exception
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            exception.getMessage()
        );
        problemDetail.setTitle("E999");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
