package com.project.moneyj.codef.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 게정 등록/추가 응답 전용 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefCredentialResultDTO(
        String connectedId,
        List<CodefCredentialSuccessDTO> successList,
        List<CodefCredentialErrorDTO> errorList
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodefCredentialSuccessDTO(
            String code,
            String message,
            String countryCode,
            String clientType,
            String organization,
            String businessType,

            String id,
            String loginType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodefCredentialErrorDTO(
            String code,
            String message,
            String organization
    ) {}
}