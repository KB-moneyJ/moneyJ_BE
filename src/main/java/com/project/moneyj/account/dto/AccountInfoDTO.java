package com.project.moneyj.account.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * 계좌 정보 응답 DTO
 * 화면 표시에 필요한 비즈니스 로직이 반영된 UI 전용 객체
 * 계좌 연결 전 사용
 */
public class AccountInfoDTO {

    private String organizationCode;
    private String accountName;
    private String accountNumber;
    private Integer balance;
}
