package com.project.moneyj.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.auth.domain.TempAuthCode;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.auth.handler.JwtAccessDenyHandler;
import com.project.moneyj.auth.handler.JwtAuthenticationEntryPoint;
import com.project.moneyj.auth.repository.TempAuthCodeRepository;
import com.project.moneyj.auth.service.CustomOAuth2UserService;
import com.project.moneyj.auth.util.JwtFilter;
import com.project.moneyj.auth.util.JwtUtil;
import com.project.moneyj.user.repository.UserRepository;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TempAuthCodeRepository tempAuthCodeRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.redirect.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 세션 대신 JWT 사용
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // JWT 인증 필터 추가 - UserRepository를 전달하고, 두 번째 인자를 Filter 클래스로 정확히 수정
            .addFilterBefore(
                new JwtFilter(jwtUtil, userRepository, objectMapper),
                UsernamePasswordAuthenticationFilter.class
            )
            // TODO: 인증 필요한 uri 추가 설정 필요
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/login/**").permitAll()
                    .requestMatchers("/users/**").authenticated()
                    .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler((request, response, authentication) -> {
                        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();

                        // 임시 토큰 발급 및 프론트로 리다이렉트
                        String tempCode = UUID.randomUUID().toString();

                        TempAuthCode tempAuthCode = TempAuthCode.of(
                            tempCode,
                            customUser.getUserId(),
                            customUser.isFirstLogin()
                        );
                        tempAuthCodeRepository.save(tempAuthCode);

                        String redirectUrl = frontendUrl + "/oauth/callback?code=" + tempCode;
                        response.sendRedirect(redirectUrl);
                    })
                    .failureHandler((request, response, exception) -> {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        try (PrintWriter writer = response.getWriter()) {
                            writer.write("{\"status\":\"fail\",\"message\":\"로그인 실패\"}");
                            writer.flush();
                        } catch (IOException e) {
                            // TODO: 예외 처리 로깅 추가
                        }
                    })
            )
            .logout(logout -> logout
                    .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI())
                        && HttpMethod.POST.matches(request.getMethod()))
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpStatus.OK.value());
                        try (PrintWriter writer = response.getWriter()) {
                            writer.write("{\"status\":\"success\",\"message\":\"로그아웃 완료\"}");
                            writer.flush();
                        } catch (IOException e) {
                            // TODO: 예외 처리 로깅 추가
                        }
                    })
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper))  // 인증 실패 처리
                .accessDeniedHandler(new JwtAccessDenyHandler(objectMapper))              // 권한 부족 처리
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
