package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저를 찾을 수 없습니다."),
    NOT_LOGGED_IN(HttpStatus.UNAUTHORIZED, "U002", "로그인이 필요합니다."); // TODO: 추후 인증 에러 코드로 옮기기

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() { return message; }
}
