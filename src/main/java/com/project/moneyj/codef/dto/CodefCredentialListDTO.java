package com.project.moneyj.codef.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 계정 목록 조회 전용 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefCredentialListDTO(
        String connectedId,
        List<CodefCredentialDTO> accountList
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodefCredentialDTO(
            String countryCode,
            String clientType,
            String organization,
            String businessType,
            String loginType
    ) {}
}