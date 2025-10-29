package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.dto.CardCredentialAddRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.codef.util.RsaEncryptor;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefCardService {

    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;        // 기존 토큰 발급/캐시 서비스
    private final CodefConnectedIdRepository connectedRepo; // 기존 CID 저장소
    private final CodefProperties props;                    // clientId/secret/publicKey/baseUrl 등
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final CardRepository cardRepository;


    /**
     * 거래 내역 조회(카드)
     */
    public Map<String, Object> getCardApprovalList(Long userId, CardApprovalRequestDTO req) {
        String accessToken = codefAuthService.getValidAccessToken();

        String connectedId = connectedRepo.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("사용자에 대한 ConnectedID가 없습니다. 먼저 생성해 주세요."));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organization", req.getOrganization());
        body.put("connectedId", connectedId);

        // 옵션들 채우기
        if (req.getBirthDate() != null && !req.getBirthDate().isBlank())
            body.put("birthDate", req.getBirthDate());
        if (req.getStartDate() != null && !req.getStartDate().isBlank())
            body.put("startDate", req.getStartDate()); // YYYYMMDD
        if (req.getEndDate() != null && !req.getEndDate().isBlank())
            body.put("endDate", req.getEndDate());     // YYYYMMDD

        body.put("orderBy", (req.getOrderBy() == null || req.getOrderBy().isBlank()) ? "0" : req.getOrderBy());          // 기본 최신순
        body.put("inquiryType", (req.getInquiryType() == null || req.getInquiryType().isBlank()) ? "1" : req.getInquiryType()); // 기본 전체조회

        // 카드별 조회일 때만 카드 식별 값 세팅
        if ("0".equals(body.get("inquiryType"))) {
            if (req.getCardName() != null && !req.getCardName().isBlank())
                body.put("cardName", req.getCardName());
            if (req.getDuplicateCardIdx() != null && !req.getDuplicateCardIdx().isBlank())
                body.put("duplicateCardIdx", req.getDuplicateCardIdx());
        }

        // KB 소지자 확인 필요한 경우만
        if (req.getCardNo() != null && !req.getCardNo().isBlank())
            body.put("cardNo", req.getCardNo());
        // 승인내역 조회 바디 구성 직전에 추가
        if (req.getCardPassword() != null && !req.getCardPassword().isBlank()) {
            // 카드비밀번호 **앞 2자리**만 평문으로 받아서 암호화
            String enc = RsaEncryptor.encryptWithPemPublicKey(req.getCardPassword(), props.getPublicKey());
            body.put("cardPassword", enc);
        }

        body.put("memberStoreInfoType",
                (req.getMemberStoreInfoType() == null || req.getMemberStoreInfoType().isBlank())
                        ? "1" : req.getMemberStoreInfoType());

        String url = props.getBaseUrl() + "/v1/kr/card/p/account/approval-list";

        String encodedResponse = codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class) // text/plain 방지용
                .block();

        return ApiResponseDecoder.decode(encodedResponse);
    }

    /**
     * 보유 카드 조회 및 DB 저장/업데이트
     */
    @Transactional
    public Map<String, Object> getOwnedCards(Long userId, String organization) {
        // 1. API 호출에 필요한 정보 준비
        String accessToken = codefAuthService.getValidAccessToken();
        String connectedId = connectedRepo.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("사용자에 대한 ConnectedID가 없습니다."));

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다. userId=" + userId));

        // 2. CODEF 보유카드 목록 조회 API 호출
        Map<String, Object> body = Map.of(
                "organization", organization,
                "connectedId", connectedId
        );

        String url = props.getBaseUrl() + "/v1/kr/card/p/account/card-list";

        String encodedResponse = codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Map<String, Object> responseMap = ApiResponseDecoder.decode(encodedResponse);

        // API 호출 실패 시 예외 처리
        Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
        String code = (String) result.get("code");
        if (!"CF-00000".equals(code)) {
            log.error("CODEF 카드 목록 조회 실패: {}", result);
            throw new IllegalStateException("CODEF 카드 목록 조회에 실패했습니다: " + result.get("message"));
        }

        // 3. 응답 데이터 파싱 및 카드 목록 추출
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        List<Map<String, Object>> cardListFromApi = extractCardListFromData(data);

        if (cardListFromApi.isEmpty()) {
            log.info("사용자 ID {} / 기관코드 {}에 대해 조회된 카드가 없습니다.", userId, organization);
        }

        // 4. DB에 카드 정보 동기화 (for-loop 사용)
        for (Map<String, Object> cardInfo : cardListFromApi) {
            String cardNo = (String) cardInfo.get("resCardNo");
            if (cardNo == null || cardNo.isBlank()) {
                continue; // 카드 번호가 없으면 건너뛰기
            }

            Optional<Card> existingCardOpt = cardRepository.findByUserAndCardNo(user, cardNo);

            if (existingCardOpt.isPresent()) {
                // 카드가 이미 존재하면 정보 업데이트 (Update)
                Card existingCard = existingCardOpt.get();
                // @Data 어노테이션으로 생성된 Setter 사용
                existingCard.setCardName((String) cardInfo.get("resCardName"));
                // organizationCode는 이미 동일하므로 업데이트 필요 없음
                cardRepository.save(existingCard);
                log.info("기존 카드 정보 업데이트: {}", cardNo);
            } else {
                // 카드가 존재하지 않으면 새로 추가 (Insert)
                Card newCard = Card.builder()
                        .user(user)
                        .cardNo(cardNo)
                        .cardName((String) cardInfo.get("resCardName"))
                        .organizationCode(organization) // 파라미터로 받은 기관 코드를 저장
                        .build();
                cardRepository.save(newCard);
                log.info("새로운 카드 정보 추가: {}", cardNo);
            }
        }
        return result;
    }

    /**
     * CODEF API 응답의 data 객체에서 카드 목록을 추출.
     * 응답이 단일 객체이거나 배열(resCardList)인 경우 모두 처리.
     */
    private List<Map<String, Object>> extractCardListFromData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }

        Object cardListObj = data.get("resCardList");
        if (cardListObj instanceof List) {
            return (List<Map<String, Object>>) cardListObj;
        }

        return List.of(data);
    }

    /**
     * CODEF 응답 처리를 위한 공통 헬퍼 메서드
     */
    private Map<String, Object> parseCodefResponse(String rawResponse) {
        log.info("Raw response from CODEF: {}", rawResponse);
        try {
            String decodedResponse = URLDecoder.decode(rawResponse, StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(decodedResponse, new TypeReference<>() {});
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
            String code = (String) result.get("code");

            if ("CF-00000".equals(code)) {
                return responseMap;
            } else {
                throw new IllegalStateException("CODEF 비즈니스 에러: " + result.get("message"));
            }
        } catch (Exception e) {
            throw new RuntimeException("CODEF 응답을 처리할 수 없습니다: " + rawResponse, e);
        }
    }

    // 공용: 안전 파서
    private Map<String, Object> readAsMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            // text/plain 등 비정형 응답일 때 디버깅 도움
            throw new IllegalStateException("CODEF 응답 파싱 실패: " + raw, e);
        }
    }
}
