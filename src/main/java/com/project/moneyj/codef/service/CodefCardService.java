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
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    // 보유 카드 조회
    public List<CodefCardDTO> fetchCards(Long userId, String organization) {
        String connectedId = codefConnectedIdRepository.findActiveConnectedIdByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        // CODEF 보유카드 목록 조회 API 호출
        Map<String, Object> body = Map.of(
                "organization", organization,
                "connectedId", connectedId
        );

        String url = codefProperties.getBaseUrl() + "/v1/kr/card/p/account/card-list";

        String rawResponse = codefApiClient.executePost(url, body);

        TypeReference<CodefResponseDTO<List<CodefCardDTO>>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<List<CodefCardDTO>> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

        log.info("보유 중인 카드 리스트: {}", ApiResponseDecoder.decode(rawResponse));

        return responseDTO.data();
    }

    // 거래 내역 조회(카드)
    public List<CodefCardApprovalDTO> getCardApprovalList(Long userId, CardApprovalRequestDTO req) {

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
        String rawResponse = codefApiClient.executePost(url, body);

        TypeReference<CodefResponseDTO<List<CodefCardApprovalDTO>>> typeRef = new TypeReference<>() {};
        CodefResponseDTO<List<CodefCardApprovalDTO>> responseDTO = ApiResponseDecoder.decode(rawResponse, typeRef);

        if (responseDTO == null || !responseDTO.result().isSuccess() || responseDTO.data() == null) {
            log.error("CODEF 카드 승인 내역 조회 실패");
            return Collections.emptyList();
        }

        return responseDTO.data();
    }
}
