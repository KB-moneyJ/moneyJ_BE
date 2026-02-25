package com.project.moneyj.codef.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.codef.service.*;
import com.project.moneyj.codef.service.facade.CodefCredentialFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/codef")
public class CodefAuthController implements CodefAuthControllerApiSpec{

    private final CodefAuthService codefAuthService;
    private final CodefCredentialFacade codefCredentialFacade;


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
     * 계정 목록 조회
     * 등록한 계정(은행사/카드사) 목록
     */
    @Override
    @GetMapping("/credentials")
    public ResponseEntity<?> listCredentials(
            @AuthenticationPrincipal CustomOAuth2User customUser) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(codefCredentialFacade.listCredentials(userId));
    }
}
