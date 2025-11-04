package com.project.moneyj.auth.controller;

import com.project.moneyj.auth.dto.SessionResponseDTO;
import com.project.moneyj.auth.dto.TempAuthCodeRequestDTO;
import com.project.moneyj.auth.dto.TokenResponse;
import com.project.moneyj.auth.service.TempAuthCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerApiSpec{

    private final TempAuthCodeService tempAuthCodeService;

    @GetMapping("/validate")
    public ResponseEntity<SessionResponseDTO> validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean isValid = session != null;
        return ResponseEntity.ok(new SessionResponseDTO(isValid));
    }

    @PostMapping("/exchange")
    public ResponseEntity<TokenResponse> exchangeTempCode(@RequestBody TempAuthCodeRequestDTO request) {
        TokenResponse response = tempAuthCodeService.exchangeTempCode(request.getCode());
        return ResponseEntity.ok(response);
    }
}
