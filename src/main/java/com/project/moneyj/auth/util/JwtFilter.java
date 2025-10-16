package com.project.moneyj.auth.util;

import com.project.moneyj.auth.config.SecurityConfig;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // UserRepository 주입

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        String userIdStr = jwtUtil.extractUsername(jwt); // 이제 username은 User의 ID입니다.

        if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.isTokenValid(jwt)) {
                // 토큰에서 추출한 userId로 DB에서 User 정보를 조회합니다.
                Long userId = Long.parseLong(userIdStr);
                User user = userRepository.findById(userId)
                        .orElse(null); // 사용자가 없을 경우 null

                if (user != null) {
                    // 조회된 User 정보로 CustomOAuth2User 객체를 생성합니다.
                    // 이 때 attributes와 isFirstLogin은 JWT 인증 시점에서는 중요하지 않으므로,
                    // 각각 null(또는 빈 Map)과 false로 설정합니다.
                    CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, Collections.emptyMap(), false);

                    // CustomOAuth2User를 Principal로 사용하는 Authentication 객체를 생성합니다.
                    Authentication authToken = new UsernamePasswordAuthenticationToken(
                            customOAuth2User,
                            null,
                            customOAuth2User.getAuthorities() // 권한 정보도 함께 설정
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}