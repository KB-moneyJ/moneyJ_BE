package com.project.moneyj.transaction.domain.event;

import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionRequestEvent {
    private final Long userId;
    private final CardApprovalRequestDTO request;
}
