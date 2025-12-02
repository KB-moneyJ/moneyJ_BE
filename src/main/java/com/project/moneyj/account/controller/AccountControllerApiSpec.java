package com.project.moneyj.account.controller;

import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountLinkResponseDTO;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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

    @Operation(
            summary = "계좌번호 저장 여부 검사",
            description = "해당 계좌번호가 현재 사용자에게 이미 저장(등록)되어 있는지 여부를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상적으로 계좌 사용 여부를 반환함"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(계좌번호 형식 오류 등)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    boolean checkAccountOwnership(
            @Parameter(description = "조회할 계좌번호", required = true, example = "123-456-789012")
            @PathVariable String accountNumber
    );

    @Operation(summary = "DB에서 계좌 삭제", description = "계좌를 DB에서 삭제합니다.(codef와 연결은 삭제 안됨)")
    ResponseEntity<Void> deleteAccount(@PathVariable Long accountId);
}
