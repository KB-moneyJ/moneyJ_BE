package com.project.moneyj.analysis.controller;

import com.project.moneyj.analysis.dto.MonthlySummaryDTO.CategorySummaryDTO;
import com.project.moneyj.analysis.dto.SummaryResponseDTO;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

public interface TransactionSummaryApiSpec {

    @Operation(summary = "월별, 카테고리별 소비 요약 내역", description = "사용자의 월별, 카테고리별 소비 요약 내역을 반환합니다. month는 (YYYY-MM) 형태여야 합니다. category에 대소문자 모두 가능합니다.")
    ResponseEntity<CategorySummaryDTO> getMonthlyCategorySummary(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestParam String month,
        @RequestParam String category
    );

    @Operation(summary = "최근 6개월 소비 요약 내역", description = "사용자의 최근 6개월 소비 요약 내역을 반환합니다. 기준 연월(YYYY-MM) 생략 시 현재 월 기준 최근 6개월이 조회됩니다.")
    ResponseEntity<SummaryResponseDTO> getRecent6MonthsSummary(
        @RequestParam(required = false) String base, // /summary?base=2025-09
        @AuthenticationPrincipal CustomOAuth2User customUser
    );

}
