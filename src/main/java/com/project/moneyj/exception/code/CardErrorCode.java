package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-001", "사용자를 찾을 수 없습니다."),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-002", "카드를 찾을 수 없습니다."),
    CARD_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARD-003", "이미 등록된 카드입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "CARD-004", "해당 카드에 대한 접근 권한이 없습니다."),
    NO_CARDS_FOUND_FROM_CODEF(HttpStatus.NOT_FOUND, "CARD-005", "CODEF 조회 결과 연동할 카드가 없습니다.");

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
}
