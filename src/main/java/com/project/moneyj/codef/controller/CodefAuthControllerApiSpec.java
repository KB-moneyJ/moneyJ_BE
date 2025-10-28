package com.project.moneyj.codef.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.codef.dto.AccountCreateRequestDTO;
import com.project.moneyj.codef.dto.AccountDeleteRequestDTO;
import com.project.moneyj.codef.dto.BankTxnListReqDTO;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CODEF API", description = "CODEF 연동 API")
public interface CodefAuthControllerApiSpec {

    @Operation(summary = "CODEF 토큰 발급 (테스트용)", description = "CODEF API 호출을 위한 엑세스 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "토큰 발급 성공")
    ResponseEntity<?> getToken();

    @Operation(summary = "커넥티드 ID 발급", description = "CODEF 계정 연결을 위한 커넥티드 ID를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ID 발급 성공"),
            @ApiResponse(responseCode = "400", description = "계정 정보가 비어있음")
    })
    ResponseEntity<?> createConnectedId(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountCreateRequestDTO request
    );

    @Operation(summary = "자격/계정 추가", description = "은행사/카드사의 아이디, 패스워드를 등록하여 계정을 추가합니다.")
    ResponseEntity<?> addCredential(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountCreateRequestDTO.AccountInput input
    ) throws Exception;

    @Operation(summary = "등록된 계정 목록 조회", description = "CODEF에 등록한 계정(은행/카드사) 목록을 조회합니다.")
    ResponseEntity<?> listCredentials(
            @AuthenticationPrincipal CustomOAuth2User customUser);

    @Operation(summary = "은행 계좌 목록 조회", description = "특정 기관(은행)의 계좌 목록을 조회합니다.")
    ResponseEntity<?> bankAccounts(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @Parameter(description = "조회할 기관 코드") @RequestParam String organization
    );

    @Operation(summary = "은행 거래 내역 조회", description = "특정 계좌의 거래 내역을 조회합니다.")
    ResponseEntity<?> bankTransactions(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody BankTxnListReqDTO req
    );

    @Operation(summary = "보유 카드 목록 조회", description = "특정 기관(카드사)의 보유 카드 목록을 조회합니다.")
    ResponseEntity<?> ownedCards(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @Parameter(description = "조회할 기관 코드") @RequestParam String organization
    );

    @Operation(summary = "카드 거래 내역 조회", description = "특정 카드의 거래 내역(청구 내역)을 조회합니다.")
    ResponseEntity<?> billing(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CardApprovalRequestDTO req
    );

    @Operation(summary = "CODEF 연결 계정 삭제", description = "CODEF에 연결된 계정(자격)을 삭제합니다.")
    ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountDeleteRequestDTO request
    );

}
