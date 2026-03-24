package com.project.moneyj.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.dto.CodefCardApprovalDTO;
import com.project.moneyj.codef.dto.CodefCardDTO;
import com.project.moneyj.codef.dto.CodefResponseDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.codef.util.RsaEncryptor;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefCardService {

    private final CodefApiClient codefApiClient;
    private final CodefProperties codefProperties;

    // 보유 카드 조회
    public List<CodefCardDTO> fetchCards(String connectedId, String organization) {

        // CODEF 보유카드 목록 조회 API 호출
        Map<String, Object> body = Map.of(
                "organization", organization,
                "connectedId", connectedId
        );

        String url = codefProperties.getBaseUrl() + "/v1/kr/card/p/account/card-list";

        List<CodefCardDTO> data = codefApiClient.fetchAndDecode(url, body, new TypeReference<>() {});

        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }

        // 빈 객체({})가 들어와서 null 밭이 된 DTO가 있다면 필터링
        return data.stream()
                .filter(card -> card.resCardNo() != null && !card.resCardNo().isBlank())
                .toList();
    }

    // 거래 내역 조회(카드)
    public List<CodefCardApprovalDTO> getCardApprovalList(String connectedId, CardApprovalRequestDTO req) {

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
        List<CodefCardApprovalDTO> data = codefApiClient.fetchAndDecode(url, body, new TypeReference<>() {});

        return data != null ? data : Collections.emptyList();
    }
}
