package com.project.moneyj.account.controller;

import com.project.moneyj.account.dto.*;
import com.project.moneyj.account.service.AccountService;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController implements AccountControllerApiSpec{

    private final AccountService accountService;

    /**
     * 은행 계좌 목록 조회 및 기관 연결
     * CODEF를 통해 기관(은행/카드사)에 연결하고, 성공 시 해당 기관의 계좌 목록을 반환
     * 최초 등록시 커넥티드 ID 발급
     * '계좌 변경' 시에도 사용
     */
    @Override
    @PostMapping("/connect")
    public ResponseEntity<List<AccountInfoDTO>> connectAndFetchAccounts(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody AccountConnectionRequestDTO request) {

        Long userId = customUser.getUserId();
        List<AccountInfoDTO> accounts = accountService.connectInstitutionAndFetchAccounts(userId, request);
        return ResponseEntity.ok(accounts);
    }

    /**
     * 사용자가 선택한 은행 계좌를 저장
     * 여행별 선택한 계좌를 DB에 저장
     */
    @Override
    @PostMapping("/link")
    public ResponseEntity<AccountResponseDTO> linkAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody @Valid AccountLinkRequestDTO request
    ) {
        Long userId = customUser.getUserId();
        AccountResponseDTO responseDto = accountService.linkUserAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 계좌 변경
     */
    @Override
    @PatchMapping("/switch/{accountId}")
    public ResponseEntity<AccountResponseDTO> switchAccount(
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
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ) {
        Long userId = customUser.getUserId();
        accountService.deleteAccount(userId, accountId);
        return ResponseEntity.ok("계좌가 성공적으로 삭제되었습니다.");
    }

    /**
     * 계좌번호 저장 여부 검사
     */
    @Override
    @GetMapping("/check/{accountNumber}")
    public boolean checkAccountOwnership(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ) {
        Long userId = customUser.getUserId();
        return accountService.checkAccountOwnership(userId, accountNumber);
    }

    /**
     * 계좌 수동 업데이트 및 조회
     */
    @Override
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDTO> manualAccountUpdate(
            @PathVariable Long accountId,
            @AuthenticationPrincipal CustomOAuth2User customUser
    ){
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(accountService.manualAccount(userId, accountId));
    }
}
