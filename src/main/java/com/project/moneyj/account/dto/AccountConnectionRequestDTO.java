package com.project.moneyj.account.dto;

/**
 * 계좌 연동/추가 요청 DTO
 */
public record AccountConnectionRequestDTO(
        String countryCode,
        String businessType,
        String clientType,
        String organization,
        String loginType,
        String id,
        String password
) {}