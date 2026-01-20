package com.project.moneyj.codef.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CODEF API", description = "CODEF 연동 API")
public interface CodefAuthControllerApiSpec {

    @Operation(summary = "CODEF 토큰 발급 (테스트용)", description = "CODEF API 호출을 위한 엑세스 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "토큰 발급 성공")
    ResponseEntity<?> getToken();

    @Operation(summary = "등록된 계정 목록 조회", description = "CODEF에 등록한 계정(은행/카드사) 목록을 조회합니다.")
    ResponseEntity<?> listCredentials(
            @AuthenticationPrincipal CustomOAuth2User customUser);
}
