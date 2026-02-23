package com.project.moneyj.transaction.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.transaction.dto.TransactionRequestDTO;
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
public class TransactionController implements TransactionControllerApiSpec{
    private final TransactionFacade transactionFacade;

    @Override
    @PostMapping("/save")
    public ResponseEntity<Void> saveCardTransactions(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody TransactionRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        transactionFacade.processTransactions(userId, request);

        return ResponseEntity.ok().build();
    }

}
