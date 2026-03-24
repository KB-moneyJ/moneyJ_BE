package com.project.moneyj.codef.service;

import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefToken;
import com.project.moneyj.codef.dto.TokenResponseDTO;
import com.project.moneyj.codef.repository.CodefTokenRepository;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class CodefAuthService {

    private final CodefProperties codefProperties;
    private final WebClient codefWebClient;
    private final CodefTokenRepository codefTokenRepository;

    /**
     * 토큰 발급
     *  - 유효 토큰 있으면 반환
     *  - 만료 임박/만료면 재발급 후 저장하고 반환
     */
    @Transactional
    public String getValidAccessToken() {
        var latestOpt = codefTokenRepository.findTopByOrderByCodefTokenIdDesc();
        var now = LocalDateTime.now();

        if (latestOpt.isPresent()) {
            CodefToken latest = latestOpt.get();
            var safeLimit = latest.getExpiresAt().minusSeconds(codefProperties.getRefreshMarginSec());
            if (now.isBefore(safeLimit)) {
                return latest.getAccessToken();
            }
            log.info("CODEF access_token 만료 임박 → 갱신 시도");
            return refresh(latest.getCodefTokenId());
        }

        log.info("CODEF access_token 없음 → 최초 발급");
        return issueFirst();
    }

    private String issueFirst() {
        TokenResponseDTO res = requestAccessToken();
        var newToken = CodefToken.of(res.getAccessToken(), LocalDateTime.now().plusSeconds(res.getExpiresIn()));
        codefTokenRepository.save(newToken);
        return newToken.getAccessToken();
    }

    private String refresh(Long idToUpdate) {
        TokenResponseDTO res = requestAccessToken();

        CodefToken token = codefTokenRepository.findById(idToUpdate)
                .orElseGet(CodefToken::empty);

        token.getToken(res);
        codefTokenRepository.save(token);

        return token.getAccessToken();
    }

    /**
     * CODEF OAuth2 Client Credentials 요청
     */
    private TokenResponseDTO requestAccessToken() {

        String url = "https://oauth.codef.io/oauth/token";

        return codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> {
                    // 기존 로직 그대로 유지
                    h.setBasicAuth(codefProperties.getClientId(), codefProperties.getClientSecret());
                })
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", codefProperties.getClientId())
                        .with("client_secret", codefProperties.getClientSecret())
                        .with("scope", "read")) // 기존 로직 그대로 유지
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("CODEF 500 에러 진짜 원인: 상태코드={}, 응답본문={}", response.statusCode(), errorBody);
                            return Mono.error(MoneyjException.of(CodefErrorCode.TOKEN_PARSE_FAILED));
                        })
                )
                .bodyToMono(TokenResponseDTO.class)
                .blockOptional()
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.TOKEN_PARSE_FAILED));
    }
}
