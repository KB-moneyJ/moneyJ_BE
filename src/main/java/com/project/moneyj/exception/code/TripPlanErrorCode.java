package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TripPlanErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "TP001", "존재하지 않는 여행 플랜입니다."),
    NO_MEMBERS_IN_PLAN(HttpStatus.NOT_FOUND, "TP002", "해당 플랜에 멤버가 존재하지 않습니다.");

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
