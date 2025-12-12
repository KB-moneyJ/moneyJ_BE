package com.project.moneyj.account.controller;

import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountLinkResponseDTO;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController implements AccountControllerApiSpec{

    private final AccountService accountService;

    /**
     * 사용자가 선택한 계좌를 저장.
     */
    @Override
    @PostMapping("/link")
    public ResponseEntity<AccountLinkResponseDTO> linkAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountLinkRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        // 서비스로부터 DTO를 직접 받음
        AccountLinkResponseDTO responseDto = accountService.linkUserAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 계좌번호 저장 여부 검사
     */
    @Override
    @GetMapping("/check/{accountNumber}")
    public boolean checkAccountOwnership(@PathVariable String accountNumber) {
        return accountService.checkAccountOwnership(accountNumber);
    }
}
