package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TripMemberErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "TM001", "해당 여행 플랜의 멤버가 아닙니다."),
    ALREADY_EXISTS(HttpStatus.CONFLICT, "TM002", "이미 여행에 참여하고 있는 멤버입니다.");


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
