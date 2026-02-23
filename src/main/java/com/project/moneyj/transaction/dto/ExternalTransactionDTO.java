package com.project.moneyj.transaction.dto;

import java.time.LocalDateTime;

/**
 * 거래내역 정보 DTO
 * CODEF 인프라 계층과 Account 도메인 계층 사이의 데이터 전달을 담당
 * 외부 API의 응답 스펙이 변경되더라도, 이 DTO를 통해 도메인 로직 및 UI 응답 규격이 변형되는 것을 방지
 */
public record ExternalTransactionDTO(
        LocalDateTime usedDateTime,
        int actualAmount,
        String storeName,
        String storeCorpNo,
        String storeAddr,
        String storeNo,
        String storeType,
        String approvalNo
) {}