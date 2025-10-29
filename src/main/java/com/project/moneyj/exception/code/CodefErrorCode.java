package com.project.moneyj.exception.code;

import com.project.moneyj.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CodefErrorCode implements ErrorCode {

    // 404 NOT_FOUND
    CONNECTED_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "CF001", "사용자의 Connected ID를 찾을 수 없습니다."),
    INSTITUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CF002", "DB에서 해당 기관 정보를 찾을 수 없습니다."),

    // 400 BAD_REQUEST (또는 비즈니스 로직 에러)
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "CF003", "CODEF 비즈니스 에러가 발생했습니다."),
    REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "CF004", "CODEF 계정 등록에 실패했습니다."),
    DELETION_FAILED(HttpStatus.BAD_REQUEST, "CF005", "CODEF 계정 삭제에 실패했습니다."),
    CARD_LIST_FAILED(HttpStatus.BAD_REQUEST, "CF006", "CODEF 카드 목록 조회에 실패했습니다."),

    // 500 INTERNAL_SERVER_ERROR (내부 처리 또는 CODEF 응답 오류)
    RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CF006", "CODEF 응답을 파싱(처리)하는데 실패했습니다."),
    EMPTY_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "CF007", "CODEF로부터 비어있는 응답을 받았습니다."),
    CONNECTED_ID_NOT_RECEIVED(HttpStatus.INTERNAL_SERVER_ERROR, "CF008", "CODEF로부터 Connected ID를 수신하지 못했습니다."),
    TOKEN_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CF010", "CODEF 토큰 응답을 파싱하는데 실패했습니다."),

    // 502 BAD_GATEWAY
    API_HTTP_ERROR(HttpStatus.BAD_GATEWAY, "CF009", "CODEF API 호출 중 HTTP 에러가 발생했습니다.");


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
