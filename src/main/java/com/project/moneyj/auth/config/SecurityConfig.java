package com.project.moneyj.auth.config;

import com.project.moneyj.auth.handler.CustomLoginFailureHandler;
import com.project.moneyj.auth.handler.CustomLoginSuccessHandler;
import com.project.moneyj.auth.handler.CustomLogoutSuccessHandler;
import com.project.moneyj.auth.handler.JwtAccessDenyHandler;
import com.project.moneyj.auth.handler.JwtAuthenticationEntryPoint;
import com.project.moneyj.auth.service.CustomOAuth2UserService;
import com.project.moneyj.auth.util.JwtFilter;
import com.project.moneyj.auth.util.JwtUtil;
import com.project.moneyj.auth.util.SecurityResponseUtil;
import com.project.moneyj.user.repository.UserRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final SecurityResponseUtil securityResponseUtil;

    @Value("${spring.redirect.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 세션 대신 JWT 사용
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // JWT 인증 필터 추가
            .addFilterBefore(
                new JwtFilter(jwtUtil, userRepository, securityResponseUtil),
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
                    .successHandler(customLoginSuccessHandler)
                    .failureHandler(customLoginFailureHandler)
            )
            .logout(logout -> logout
                    .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI())
                        && HttpMethod.POST.matches(request.getMethod()))
                    .logoutSuccessHandler(customLogoutSuccessHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint(securityResponseUtil)) // 인증 실패 처리
                .accessDeniedHandler(new JwtAccessDenyHandler(securityResponseUtil)) // 권한 부족 처리
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
