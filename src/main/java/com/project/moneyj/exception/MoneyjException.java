package com.project.moneyj.exception;

import lombok.Getter;

@Getter
public class MoneyjException extends RuntimeException {
    private final ErrorCode errorCode;

    private MoneyjException(ErrorCode errorCode) {
        super(errorCode.message()); // RuntimeException 객체에 에러 메세지 전달
        this.errorCode = errorCode;
    }

    public static MoneyjException of(ErrorCode errorCode) {
        return new MoneyjException(errorCode);
    }
}
