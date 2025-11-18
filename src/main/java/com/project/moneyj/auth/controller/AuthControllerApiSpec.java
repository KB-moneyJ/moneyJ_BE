package com.project.moneyj.auth.controller;

import com.project.moneyj.auth.dto.SessionResponseDTO;
import com.project.moneyj.auth.dto.TempAuthCodeRequestDTO;
import com.project.moneyj.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthControllerApiSpec {

    @Operation(summary = "세션 유효 여부", description = "사용자의 세션 유효 여부를 반환합니다.")
    ResponseEntity<SessionResponseDTO> validateSession(HttpServletRequest request);

    @Operation(summary = "임시 토큰으로 jwt 발급", description = "jwt 토큰을 발급합니다.")
    ResponseEntity<TokenResponse> exchangeTempCode(@RequestBody TempAuthCodeRequestDTO request);

}
