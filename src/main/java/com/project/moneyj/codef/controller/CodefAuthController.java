package com.project.moneyj.codef.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.codef.dto.*;
import com.project.moneyj.codef.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/codef")
public class CodefAuthController implements CodefAuthControllerApiSpec{


    private final CodefAuthService codefAuthService;
    private final CodefAccountService accountService;
    private final CodefCredentialService credSvc;
    private final CodefBankService bankSvc;
    private final CodefCardService cardService;


    /**
     * 토큰 발급
     */
    @Override
    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        String token = codefAuthService.getValidAccessToken();
        String masked = token.length() > 12 ? token.substring(0, 12) + "..." : token;
        return ResponseEntity.ok().body("{\"accessToken\":\"" + masked + "\"}");
    }

    /**
     * 커넥티드 ID 발급
     */
    @Override
    @PostMapping("/connected-id")
    public ResponseEntity<?> createConnectedId(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountCreateRequestDTO request){

        // 요청 객체나 리스트가 비어있는지 확인하는 방어 코드
        if (request == null || request.getAccountList() == null || request.getAccountList().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"계정 정보가 비어있습니다.\"}");
        }

        // 리스트에서 첫 번째 AccountInput 객체를 꺼내서 서비스로 전달합니다.
        AccountCreateRequestDTO.AccountInput input = request.getAccountList().get(0);

        Long userId = customUser.getUserId();

        String connectedId = accountService.createConnectedId(userId, input);
        return ResponseEntity.ok().body("{\"connectedId\":\"" + connectedId + "\"}");
    }

    /**
     * 계정 추가
     * 은행사/카드사의 아이디, 패스워드를 통한 계정 추가
     */
    @Override
    @PostMapping("/credentials")
    public ResponseEntity<?> addCredential(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountCreateRequestDTO.AccountInput input) throws Exception {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(credSvc.addCredential(userId, input));
    }

    /**
     * 계정 목록 조회
     * 등록한 계정(은행사/카드사) 목록
     */
    @Override
    @GetMapping("/credentials")
    public ResponseEntity<?> listCredentials(
            @AuthenticationPrincipal CustomOAuth2User customUser) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(credSvc.listCredentials(userId));
    }

    /**
     * 은행 계좌 목록 조회
     */
    @Override
    @GetMapping("/bank/accounts")
    public ResponseEntity<?> bankAccounts(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestParam String organization) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(bankSvc.fetchBankAccounts(userId, organization));
    }

    /**
     * 은행 거래 내역
     */
    // TODO 불필요시 삭제
    @Override
    @PostMapping("/bank/transactions")
    public ResponseEntity<?> bankTransactions(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody BankTxnListReqDTO req) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(bankSvc.fetchTransactions(userId, req));
    }

    /**
     * 보유 카드 목록 조회
     */
    @Override
    @GetMapping("/owned")
    public ResponseEntity<?> ownedCards(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestParam String organization) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(cardService.getOwnedCards(userId, organization));
    }

    /**
     * 카드 거래 내역 조회
     */
    @Override
    @PostMapping("/billing")
    public ResponseEntity<?> billing(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CardApprovalRequestDTO req) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(cardService.getCardApprovalList(userId, req));
    }


    /**
     * CODEF에 연결된 계좌(자격)를 삭제.
     */
    @Override
    @DeleteMapping("/delete") // DB 리소스 삭제가 아니므로 POST 방식 사용
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountDeleteRequestDTO request
    ) {
        Long userId = customUser.getUserId();

        return ResponseEntity.ok(accountService.deleteAccountFromCodef(userId, request));
    }
}
