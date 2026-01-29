package com.project.moneyj.codef.service;

import com.project.moneyj.codef.config.CodefProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Log4j2
@Component
@RequiredArgsConstructor
public class CodefApiClient {

    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;
    private final CodefProperties codefProperties;

    /**
     * Codef API POST 요청 실행
     * @param path API 엔드포인트 경로
     * @param body 요청 본문
     * @return 원본 API 응답 String
     */
    public String executePost(String path, Object body) {
        String url = codefProperties.getBaseUrl() + path;
        String token = codefAuthService.getValidAccessToken();

        String rawResponse =  codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Raw response from Codef API ({}): {}", path, rawResponse);

        return rawResponse;
    }
}
