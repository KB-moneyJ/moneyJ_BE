package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    INVALID_TEMP_AUTH_CODE(HttpStatus.BAD_REQUEST, "A001", "유효하지 않은 임시 코드입니다."),
    EXPIRED_TEMP_AUTH_CODE(HttpStatus.BAD_REQUEST, "A002", "만료된 임시 코드입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public HttpStatus httpStatus() { return httpStatus; }

    @Override
    public String code() { return code; }

    @Override
    public String message() { return message; }
}
