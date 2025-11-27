package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {


    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACC-001", "해당 계좌(%s)를 목록에서 찾을 수 없습니다."),
    USER_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACC-002", "해당 유저의 계좌가 존재하지 않습니다."),
    ACCOUNT_ALREADY_IN_USE(HttpStatus.NOT_FOUND, "ACC-003", "이미 해당 계좌가 사용 중입니다.");

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
    public String message() {
        return message;
    }

    public String format(Object... args) {
        return String.format(this.message, args);
    }
}
