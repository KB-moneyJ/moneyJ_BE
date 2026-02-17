package com.project.moneyj.codef.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialDeleteRequestDTO {
    private String organizationCode; // 삭제할 계좌의 기관 코드 (ex: "004")
    private String businessType;     // 삭제할 계좌의 업무 구분 ("BK": 은행, "CD": 카드)
    private String loginType;
}
