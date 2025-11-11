package com.project.moneyj.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.exception.ErrorCode;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.AuthErrorCode;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // UserRepository 주입
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = extractJwtFromHeader(request);

        // 토큰 없거나 이미 인증된 경우 필터 생략
        if (jwt == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 내부에서 토큰의 서명, 형식, 만료 검증이 모두 이루어짐
            String userIdStr = jwtUtil.extractUsername(jwt);

            if (userIdStr == null || userIdStr.isBlank()) {
                throw MoneyjException.of(AuthErrorCode.INVALID_JWT_TOKEN);
            }

            setAuthentication(Long.parseLong(userIdStr));

        } catch (MoneyjException e) {
            setErrorResponse(request, response, e.getErrorCode());
            return;
        } catch (NumberFormatException e) { // userIdStr이 숫자가 아닐 경우
            setErrorResponse(request, response, AuthErrorCode.INVALID_JWT_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void setAuthentication(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> MoneyjException.of(AuthErrorCode.USER_NOT_FOUND));

        CustomOAuth2User customUser = new CustomOAuth2User(user, Collections.emptyMap(), false);
        Authentication authToken = new UsernamePasswordAuthenticationToken(
            customUser,
            null,
            customUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }


    private void setErrorResponse(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            errorCode.httpStatus(),
            errorCode.message()
        );
        problemDetail.setTitle(errorCode.code());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(errorCode.httpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String jsonErrorResponse = objectMapper.writeValueAsString(problemDetail);
        response.getWriter().write(jsonErrorResponse);
    }
}