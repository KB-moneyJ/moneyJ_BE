package com.project.moneyj.codef.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 카드 거래 내역 응답 전용 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefCardApprovalDTO(
        String resUsedDate,         // 승인일자
        String resUsedTime,         // 승인시간
        String resCardNo,           // 카드번호
        String resMemberStoreName,  // 가맹점명
        String resUsedAmount,       // 이용금액
        String resApprovalNo,       // 승인번호
        String resCancelYN,         // 취소여부 ("0"이면 정상, 그 외 취소)
        String resCancelAmount,     // 취소금액
        String resMemberStoreType,  // 가맹점 업종 (카테고리 매핑용)
        String resMemberStoreCorpNo,// 가맹점 사업자번호
        String resMemberStoreAddr,  // 가맹점 주소
        String resMemberStoreNo     // 가맹점 번호
) {
    // 날짜 파싱
    public LocalDateTime getUsedDateTime() {
        if (resUsedDate == null || resUsedTime == null) {
            return LocalDateTime.now(); // 혹은 null 등 정책에 맞게 처리
        }
        return LocalDateTime.parse(
                resUsedDate + resUsedTime,
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
    }

    // 실제 승인/취소 금액 계산 로직
    public int getActualAmount() {
        int usedAmount = safeParseInt(resUsedAmount);
        int cancelAmount = safeParseInt(resCancelAmount);

        return "0".equals(resCancelYN)
                ? usedAmount
                : (cancelAmount > 0 ? usedAmount - cancelAmount : usedAmount);
    }

    // 헬퍼 메서드
    private int safeParseInt(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}