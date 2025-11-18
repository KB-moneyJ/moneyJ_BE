package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    INVALID_TEMP_AUTH_CODE(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 임시 코드입니다."),
    EXPIRED_TEMP_AUTH_CODE(HttpStatus.UNAUTHORIZED, "A002", "만료된 임시 코드입니다."),
    JWT_TOKEN_IS_EMPTY(HttpStatus.BAD_REQUEST, "A003", "요청에 jwt 토큰이 없습니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 jwt 토큰입니다."),
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "올바른 jwt 토큰이 필요합니다."),
    FORBIDDEN_CLIENT(HttpStatus.FORBIDDEN, "A006", "접근 권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A007", "토큰에 해당하는 사용자가 존재하지 않습니다."),
    OAUTH2_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A008", "OAuth2 로그인에 실패했습니다.");

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
