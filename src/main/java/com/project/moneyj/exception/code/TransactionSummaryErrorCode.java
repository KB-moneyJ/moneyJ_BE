package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TransactionSummaryErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "TS001", "소비 요약 내역을 찾을 수 없습니다.");

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
