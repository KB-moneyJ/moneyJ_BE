package com.project.moneyj.auth.controller;

import com.project.moneyj.auth.dto.SessionResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerApiSpec{

    @GetMapping("/validate")
    public ResponseEntity<SessionResponseDTO> validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean isValid = session != null;
        return ResponseEntity.ok(new SessionResponseDTO(isValid));
    }
}
