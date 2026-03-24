package com.project.moneyj.transaction.dto;

import lombok.Builder;

@Builder
public record TransactionRequestDTO(
        String organization,
        String startDate,
        String endDate,
        String orderBy,
        String inquiryType,
        String cardName,
        String cardNo,
        String cardPassword
) {}