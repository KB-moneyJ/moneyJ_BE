package com.project.moneyj.account.controller;

import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountLinkResponseDTO;
import com.project.moneyj.account.dto.AccountSwitchRequestDTO;
import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.dto.UserBalanceResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 계좌 변경
     */
    @Override
    @PatchMapping("switch/{accountId}")
    public ResponseEntity<AccountLinkResponseDTO> switchAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountSwitchRequestDTO accountSwitchRequestDTO
            ) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(accountService.switchAccount(userId, accountId, accountSwitchRequestDTO));
    }

    /**
     * 계좌 삭제
     */
    @Override
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long accountId
    ) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 계좌번호 저장 여부 검사
     */
    @Override
    @GetMapping("/check/{accountNumber}")
    public boolean checkAccountOwnership(
            @PathVariable String accountNumber
    ) {
        return accountService.checkAccountOwnership(accountNumber);
    }

    /**
     * 계좌 수동 업데이트 및 조회
     */
    @Override
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountLinkResponseDTO> manualAccountUpdate(
            @PathVariable Long accountId
    ){
        return ResponseEntity.ok(accountService.manualAccount(accountId));
    }
}
