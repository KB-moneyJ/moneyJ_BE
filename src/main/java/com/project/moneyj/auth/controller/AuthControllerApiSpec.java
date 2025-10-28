package com.project.moneyj.auth.controller;

import com.project.moneyj.auth.dto.SessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AuthControllerApiSpec {

    @Operation(summary = "세션 유효 여부", description = "사용자의 세션 유효 여부를 반환합니다.")
    ResponseEntity<SessionResponseDTO> validateSession(HttpServletRequest request);

}
