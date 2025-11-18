package com.project.moneyj.analysis.controller;

import com.project.moneyj.analysis.dto.MonthlySummaryDTO.CategorySummaryDTO;
import com.project.moneyj.analysis.dto.SummaryResponseDTO;
import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.auth.dto.CustomOAuth2User;
import java.time.YearMonth;
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
public class TransactionSummaryController implements TransactionSummaryControllerApiSpec {
    private final TransactionSummaryService transactionSummaryService;

    @GetMapping()
    public ResponseEntity<SummaryResponseDTO> getRecent6MonthsSummary(
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
        return ResponseEntity.ok(
            transactionSummaryService.getMonthlyCategorySummary(customUser.getUserId(), month, category)
        );
    }

}
