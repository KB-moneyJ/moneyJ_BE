package com.project.moneyj.transaction.controller;

import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.transaction.service.TransactionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionFacade transactionFacade;
    private final TransactionSummaryService transactionSummaryService;

    @PostMapping("/save")
    public ResponseEntity<Void> saveCardTransactions(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody CardApprovalRequestDTO req
    ) {
        Long userId = customUser.getUserId();
        transactionFacade.processTransactions(userId, req);
        transactionSummaryService.initialize6MonthsSummary(userId);
        return ResponseEntity.ok().build();
    }

}
