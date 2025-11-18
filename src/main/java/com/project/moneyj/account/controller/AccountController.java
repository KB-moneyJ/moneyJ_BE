package com.project.moneyj.account.controller;

import com.project.moneyj.account.Service.AccountService;
import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountLinkResponseDTO;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/codef/accounts")
public class AccountController implements AccountControllerApiSpec{

    private final AccountService accountService;

    /**
     * 사용자가 선택한 계좌를 저장.
     */
    @PostMapping("/bank")
    public ResponseEntity<AccountLinkResponseDTO> linkAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountLinkRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        // 서비스로부터 DTO를 직접 받음
        AccountLinkResponseDTO responseDto = accountService.linkUserAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}
