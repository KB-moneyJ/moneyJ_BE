package com.project.moneyj.codef.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Codef 공통 응답 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefResponseDTO<T>(
        CodefResult result,
        T data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodefResult(
            String code,
            String message,
            String extraMessage
    ) {
        public boolean isSuccess() {
            return "CF-00000".equals(code);
        }
    }
}
