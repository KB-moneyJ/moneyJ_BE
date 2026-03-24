package com.project.moneyj.account.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * 계좌 공통 응답 DTO
 * 사용자가 선택한 계좌가 DB에 성공적으로 저장/연결 되었음을 나타내며, accountId를 포함
 * 계좌 연결 후 사용
 */
public class AccountResponseDTO {

    private Long accountId;
    private String accountName;         // 계좌명
    private String accountNumber; // 표시용 계좌번호
    private Integer balance;               // 잔액
}
