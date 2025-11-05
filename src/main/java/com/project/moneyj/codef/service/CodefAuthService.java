package com.project.moneyj.codef.service;

import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.domain.CodefToken;
import com.project.moneyj.codef.dto.TokenResponseDTO;
import com.project.moneyj.codef.repository.CodefTokenRepository;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class CodefAuthService {

    private final CodefProperties props;
    private final WebClient codefWebClient;
    private final CodefTokenRepository tokenRepository;

    /**
     * 외부에서 사용하는 진입점:
     *  - 유효 토큰 있으면 반환
     *  - 만료 임박/만료면 재발급 후 저장하고 반환
     */
    @Transactional
    public String getValidAccessToken() {
        var latestOpt = tokenRepository.findTopByOrderByCodefTokenIdDesc();
        var now = LocalDateTime.now();

        if (latestOpt.isPresent()) {
            CodefToken latest = latestOpt.get();
            var safeLimit = latest.getExpiresAt().minusSeconds(props.getRefreshMarginSec());
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
        tokenRepository.save(newToken);
        return newToken.getAccessToken();
    }

    private String refresh(Long idToUpdate) {
        TokenResponseDTO res = requestAccessToken();

        CodefToken token = tokenRepository.findById(idToUpdate)
                .orElseGet(CodefToken::new);

        token.getToken(res);
        tokenRepository.save(token);

        return token.getAccessToken();
    }

    /**
     * CODEF OAuth2 Client Credentials 요청
     * POST {baseUrl}/oauth/token
     * - Basic Auth (clientId, clientSecret)
     * - Content-Type: application/x-www-form-urlencoded
     * - body: grant_type=client_credentials
     */
    private TokenResponseDTO requestAccessToken() {

        // 1) 토큰 URL을 명시적으로 분리(원하면 oauth.codef.io로 설정)
        String url = "https://oauth.codef.io/oauth/token";

        return codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> {
                    // 2) BasicAuth도 그대로 사용 (문서가 Basic 요구하면 유지)
                    h.setBasicAuth(props.getClientId(), props.getClientSecret());
                })
                // 3) Postman과 동일하게 body에 값 추가
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", props.getClientId())
                        .with("client_secret", props.getClientSecret())
                        .with("scope", "read")) // scope 필요 없으면 제거 가능
                .retrieve()
                .bodyToMono(TokenResponseDTO.class)
                .blockOptional()
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.TOKEN_PARSE_FAILED));
    }
}
