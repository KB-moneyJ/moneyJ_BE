package com.project.moneyj.account.controller;

import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountLinkResponseDTO;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Accounts", description = "여행 플랜 계좌 API")
public interface AccountControllerApiSpec {

    @Operation(summary = "여행별 선택한 계좌를 DB에 저장", description = "사용자가 선택한 계좌를 DB에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "계좌 저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<AccountLinkResponseDTO> linkAccount(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody AccountLinkRequestDTO request
    );
}
