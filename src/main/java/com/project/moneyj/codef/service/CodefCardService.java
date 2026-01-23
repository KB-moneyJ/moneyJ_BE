package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.codef.util.RsaEncryptor;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefCardService {

    private final WebClient codefWebClient;
    private final CodefAuthService codefAuthService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;
    private final CodefProperties codefProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final CardRepository cardRepository;


    // 거래 내역 조회(카드)
    public Map<String, Object> getCardApprovalList(Long userId, CardApprovalRequestDTO req) {
        String accessToken = codefAuthService.getValidAccessToken();

        String connectedId = codefConnectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

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
            String enc = RsaEncryptor.encryptWithPemPublicKey(req.getCardPassword(), codefProperties.getPublicKey());
            body.put("cardPassword", enc);
        }

        body.put("memberStoreInfoType",
                (req.getMemberStoreInfoType() == null || req.getMemberStoreInfoType().isBlank())
                        ? "1" : req.getMemberStoreInfoType());

        String url = codefProperties.getBaseUrl() + "/v1/kr/card/p/account/approval-list";

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

    // 보유 카드 조회
    public Map<String, Object> fetchCards(Long userId, String organization) {
        String connectedId = codefConnectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        // CODEF 보유카드 목록 조회 API 호출
        Map<String, Object> body = Map.of(
                "organization", organization,
                "connectedId", connectedId
        );

        String accessToken = codefAuthService.getValidAccessToken();
        String url = codefProperties.getBaseUrl() + "/v1/kr/card/p/account/card-list";

        String encodedResponse = codefWebClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("card-list raw={}", encodedResponse);

        return ApiResponseDecoder.decode(encodedResponse);
    }
}
