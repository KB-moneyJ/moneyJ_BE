package com.project.moneyj.codef.service;

import com.project.moneyj.codef.config.CodefProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Log4j2
@Component
@RequiredArgsConstructor
public class CodefApiClient {

    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;
    private final CodefProperties codefProperties;

    /**
     * Codef API POST 요청 실행
     * @param url API 엔드포인트 경로
     * @param body 요청 본문
     * @return 원본 API 응답 String
     */
    public String executePost(String url, Object body) {
        String token = codefAuthService.getValidAccessToken();

        return codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(resp -> {
                    Mono<String> bodyMono = resp.bodyToMono(String.class).defaultIfEmpty("");

                    if (resp.statusCode().isError()) {
                        // 에러 상태 코드일 경우 (4xx, 5xx)
                        return bodyMono.doOnNext(errorBody ->
                                log.error("CODEF API Error: status={}, url={}, body={}",
                                        resp.statusCode(),
                                        url,
                                        errorBody) // 에러 본문 전체를 로그로 남김
                        );
                    } else {
                        // 성공 상태 코드일 경우
                        return bodyMono.doOnNext(successBody -> {
                            // 성공 로그는 DEBUG 레벨로 남겨서 평소에는 보이지 않게 함
                            log.debug("CODEF API Success: status={}, url={}", resp.statusCode(), url);
                            log.debug("CODEF raw body preview (first 100)={}",
                                    successBody.length() > 100 ? successBody.substring(0, 100) : successBody);
                        });
                    }
                })
                .block();
    }
}
