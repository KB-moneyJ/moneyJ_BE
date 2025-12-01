package com.project.moneyj.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.auth.domain.TempAuthCode;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.auth.repository.TempAuthCodeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TempAuthCodeRepository tempAuthCodeRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.redirect.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();

        String tempCode = UUID.randomUUID().toString();
        TempAuthCode tempAuthCode = TempAuthCode.of(
            tempCode,
            customUser.getUserId(),
            customUser.isFirstLogin()
        );
        tempAuthCodeRepository.save(tempAuthCode);

        String redirectUrl = frontendUrl + "/oauth/callback?code=" + tempCode;
        response.sendRedirect(redirectUrl);
    }

}
