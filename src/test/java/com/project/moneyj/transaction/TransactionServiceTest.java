package com.project.moneyj.transaction;

import com.project.moneyj.analysis.service.TransactionSummaryService;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.transaction.domain.Transaction;
import com.project.moneyj.transaction.repository.TransactionRepository;
import com.project.moneyj.transaction.service.TransactionService;
import com.project.moneyj.user.domain.Role;
import com.project.moneyj.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CodefCardService codefCardService;

    @Mock
    private TransactionSummaryService transactionSummaryService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("주간 거래 갱신 시 CODEF 호출 → 거래 저장 → summary 갱신 수행")
    void updateWeeklyTransactions_success_flow() {

        // given
        User user = User.of(
                "user",
                "user@test.com",
                "profile.png",
                Role.ROLE_USER
        );
        ReflectionTestUtils.setField(user, "userId", 1L);

        CardApprovalRequestDTO req = CardApprovalRequestDTO.builder()
                .organization("004")
                .startDate("20240101")
                .endDate("20240107")
                .orderBy("0")
                .inquiryType("1")
                .build();

        Map<String, Object> rawTx = Map.ofEntries(
                Map.entry("resUsedDate", "20240105"),
                Map.entry("resUsedTime", "120000"),
                Map.entry("resUsedAmount", "10000"),
                Map.entry("resCancelYN", "0"),
                Map.entry("resCancelAmount", "0"),
                Map.entry("resMemberStoreType", "편의점"),
                Map.entry("resMemberStoreName", "GS25"),
                Map.entry("resMemberStoreCorpNo", "123"),
                Map.entry("resMemberStoreAddr", "서울"),
                Map.entry("resMemberStoreNo", "001"),
                Map.entry("resApprovalNo", "APPROVAL1")
        );

        when(codefCardService.getCardApprovalList(eq(1L), any()))
                .thenReturn(Map.of("data", List.of(rawTx)));

        // when
        transactionService.updateWeeklyTransactions(user, req);

        // then
        verify(codefCardService)
                .getCardApprovalList(eq(1L), eq(req));

        ArgumentCaptor<List<Transaction>> txCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(transactionRepository)
                .saveAll(txCaptor.capture());

        List<Transaction> saved = txCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUsedAmount()).isEqualTo(10000);

        verify(transactionSummaryService)
                .updateCurrentMonthSummary(eq(1L), any(List.class));
    }
}