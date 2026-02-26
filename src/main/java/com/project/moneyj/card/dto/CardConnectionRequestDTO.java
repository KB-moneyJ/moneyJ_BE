package com.project.moneyj.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public record CardConnectionRequestDTO(
        String countryCode,
        String businessType,
        String clientType,
        String organization,
        String loginType,
        String id,
        String password
) {}