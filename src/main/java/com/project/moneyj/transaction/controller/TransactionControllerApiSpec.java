package com.project.moneyj.transaction.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transactions", description = "거래 내역 API")
@RequestMapping("/transactions")
public interface TransactionControllerApiSpec {

    @Operation(summary = "카드 거래 내역 저장", description = "CODEF에서 조회한 카드 승인 내역(거래 내역)을 서비스 DB에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/save")
    ResponseEntity<Void> saveCardTransactions(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CardApprovalRequestDTO req);

}
