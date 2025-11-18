package com.project.moneyj.codef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moneyj.codef.config.CodefProperties;
import com.project.moneyj.codef.dto.BankAccountListReqDTO;
import com.project.moneyj.codef.dto.BankTxnListReqDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.util.ApiResponseDecoder;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodefBankService {

    private final WebClient codefWebClient;
    private final CodefProperties props;
    private final CodefAuthService authService;
    private final CodefConnectedIdRepository cidRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    // 3-1) 계좌 목록
    public Map<String, Object> fetchBankAccounts(Long userId, String organization) {
        String cid = cidRepo.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED))
                .getConnectedId();

        BankAccountListReqDTO req = BankAccountListReqDTO.builder()
                .countryCode("KR").businessType("BK").clientType("P")
                .organization(organization)
                .connectedId(cid)
                .build();

        String token = authService.getValidAccessToken();
        // 예시 경로 (필요 시 문서 기준으로 조정)
        String url = props.getBaseUrl() + "/v1/kr/bank/p/account/account-list";

        String raw = codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("bank account-list raw={}", raw);

        return ApiResponseDecoder.decode(raw);
    }

    // 3-2) 거래내역
    public Map<String, Object> fetchTransactions(Long userId, BankTxnListReqDTO req) {
        String cid = cidRepo.findByUserId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_RECEIVED))
                .getConnectedId();

        // 기본값/호환 보정
        if (!StringUtils.hasText(req.getInquiryType())) {
            req.setInquiryType("1"); // 문서 예시대로 기본 1
        }
        if (StringUtils.hasText(req.getOrderBy())) {
            String ob = req.getOrderBy();
            if ("desc".equalsIgnoreCase(ob))      req.setOrderBy("1");
            else if ("asc".equalsIgnoreCase(ob))  req.setOrderBy("0");
            else if (!"0".equals(ob) && !"1".equals(ob)) req.setOrderBy("1"); // 기본 desc
        } else {
            req.setOrderBy("1"); // 기본 desc
        }

        // CODEF 요청 바디 구성 (스펙 필드만)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connectedId", cid);
        body.put("organization", req.getOrganization());
        body.put("account", req.getAccount());
        body.put("startDate", req.getStartDate());
        body.put("endDate", req.getEndDate());
        body.put("orderBy", req.getOrderBy());     // "0"|"1"
        body.put("inquiryType", req.getInquiryType());
        body.put("clientType", "P");
        if (req.getPageNo() != null) body.put("pageNo", req.getPageNo());
        if (req.getLimit()  != null) body.put("limit",  req.getLimit());

        String url = props.getBaseUrl() + "/v1/kr/bank/p/account/transaction-list";
        String token = authService.getValidAccessToken();

        String encodeResponse = codefWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ApiResponseDecoder.decode(encodeResponse);
    }
}
