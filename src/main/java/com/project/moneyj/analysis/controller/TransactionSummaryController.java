package com.project.moneyj.analysis.controller;

import com.project.moneyj.analysis.dto.MonthlySummaryDTO;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO.CategorySummaryDTO;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.transaction.domain.TransactionCategory;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/summary")
public class TransactionSummaryController {
    private final TransactionSummaryService transactionSummaryService;

    @GetMapping()
    public ResponseEntity<List<MonthlySummaryDTO>> getRecent6MonthsSummary(
        @RequestParam(required = false) String base, // /summary?base=2025-09
        @AuthenticationPrincipal CustomOAuth2User customUser
    ) {
        if (base == null || base.isEmpty()) {
            base = YearMonth.now().toString(); // 기본값: 이번 달
        }

        return ResponseEntity.ok(
            transactionSummaryService.getRecent6MonthsSummary(customUser.getUserId(), base)
        );
    }

    @GetMapping("/category")
    public ResponseEntity<CategorySummaryDTO> getMonthlyCategorySummary(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestParam String month,
        @RequestParam String category
    ) {
        TransactionCategory categoryEnum = TransactionCategory.valueOf(category.toUpperCase());

        return transactionSummaryService
            .getMonthlyCategorySummary(customUser.getUserId(), month, categoryEnum)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
