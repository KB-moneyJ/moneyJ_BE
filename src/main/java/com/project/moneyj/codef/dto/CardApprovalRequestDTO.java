package com.project.moneyj.codef.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardApprovalRequestDTO {

    // 필수
    private String organization;          // 카드사 기관코드 (예: 0300)

    // 선택 (기관/조건별)
    private String birthDate;             // YYYYMMDD (일부 기관 필수)
    private String startDate;             // YYYYMMDD (기간 조회 시작)
    private String endDate;               // YYYYMMDD (기간 조회 끝)

    /**
     * 정렬: "0"=최신순(기본), "1"=과거순
     */
    private String orderBy;               // null이면 서비스에서 "0"으로 보정

    /**
     * 조회구분: "0"=카드별 조회(특정 카드), "1"=전체조회(기본)
     */
    private String inquiryType;           // null이면 서비스에서 "1"으로 보정(전체)

    // inquiryType="0"(카드별 조회)일 때 활용
    private String cardName;              // 보유카드 조회 응답의 카드명
    private String duplicateCardIdx;      // 동일 카드명 중 복수 존재 시 식별번호

    // KB 카드 소지자 확인 필요한 경우만
    private String cardNo;                // 카드번호(전문에선 평문 요구)
    private String cardPassword;          // 카드비밀번호 앞 2자리

    /**
     * 가맹점/부가세 포함 여부:
     * "0"=미포함(기본), "1"=가맹점 포함, "2"=부가세 포함, "3"=둘 다 포함
     */
    private String memberStoreInfoType;   // null이면 서비스에서 "0"으로 보정
}
