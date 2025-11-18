package com.project.moneyj.analysis.service;


import com.project.moneyj.analysis.domain.TransactionSummary;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO;
import com.project.moneyj.analysis.dto.MonthlySummaryDTO.CategorySummaryDTO;
import com.project.moneyj.analysis.dto.SummaryResponseDTO;
import com.project.moneyj.analysis.repository.TransactionSummaryRepository;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.TransactionErrorCode;
import com.project.moneyj.exception.code.TransactionSummaryErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.transaction.domain.Transaction;
import com.project.moneyj.transaction.domain.TransactionCategory;
import com.project.moneyj.transaction.repository.TransactionRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionSummaryService {

    private final TransactionSummaryRepository transactionSummaryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SummaryResponseDTO getRecent6MonthsSummary(Long userId, String baseYearMonth) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));
        boolean idCardConnected = user.isCardConnected();

        // 6개월 요약 데이터 리스트를 조회
        List<MonthlySummaryDTO> summaries = getMonthlySummary(userId, baseYearMonth);

        return SummaryResponseDTO.of(idCardConnected, summaries);
    }

    public List<MonthlySummaryDTO> getMonthlySummary(Long userId, String baseYearMonth) {
        YearMonth base = YearMonth.parse(baseYearMonth);
        // 최근 6개월 리스트
        List<YearMonth> last6Months = IntStream.rangeClosed(0, 5)
            .mapToObj(i -> base.minusMonths(5 - i))
            .toList();

        // DB에서 해당 기간 TransactionSummary 조회
        List<TransactionSummary> summaries = transactionSummaryRepository.findByUserIdBetweenMonths(
            userId,
            last6Months.get(0).toString(),
            last6Months.get(5).toString()
        );

        // 월별 그룹핑
        Map<String, List<TransactionSummary>> grouped = summaries.stream()
            .collect(Collectors.groupingBy(
                TransactionSummary::getSummaryMonth,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        return last6Months.stream()
            .map(month -> {
                String monthStr = month.toString();
                List<TransactionSummary> monthSummaries = grouped.getOrDefault(monthStr, Collections.emptyList());

                List<MonthlySummaryDTO.CategorySummaryDTO> categories = monthSummaries.stream()
                    .map(s -> new MonthlySummaryDTO.CategorySummaryDTO(
                        s.getTransactionCategory().getDescription(),
                        s.getTotalAmount(),
                        s.getTransactionCount()
                    ))
                    .sorted(Comparator.comparingInt(MonthlySummaryDTO.CategorySummaryDTO::getTotalAmount).reversed())
                    .toList();

                int monthTotal = categories.stream()
                    .mapToInt(MonthlySummaryDTO.CategorySummaryDTO::getTotalAmount)
                    .sum();

                return new MonthlySummaryDTO(monthStr, monthTotal, categories);
            })
            .toList();
    }

    // 처음 연결한 유저: 최근 6개월 요약 생성
    @Transactional
    public void initialize6MonthsSummary(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        YearMonth base = YearMonth.now();
        YearMonth from = base.minusMonths(5); // 최근 6개월 시작

        // 최근 6개월 거래 조회
        List<Transaction> transactions = transactionRepository.findByUser_UserIdAndUsedDateTimeBetween(
            userId,
            from.atDay(1).atStartOfDay(),
            base.atEndOfMonth().atTime(LocalTime.MAX)
        );

        if (transactions.isEmpty()) return;

        // 월 + 카테고리 단위 그룹화
        Map<YearMonth, Map<TransactionCategory, List<Transaction>>> grouped = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> YearMonth.from(t.getUsedDateTime()),
                Collectors.groupingBy(Transaction::getTransactionCategory)
            ));

        List<TransactionSummary> summaries = new ArrayList<>();
        for (var monthEntry : grouped.entrySet()) {
            YearMonth month = monthEntry.getKey();
            for (var catEntry : monthEntry.getValue().entrySet()) {
                TransactionCategory category = catEntry.getKey();
                List<Transaction> txs = catEntry.getValue();

                summaries.add(TransactionSummary.of(
                        null,
                        user,
                        category,
                        month.toString(),
                        txs.stream().mapToInt(Transaction::getUsedAmount).sum(),
                        txs.size(),
                        LocalDate.now()
                ));
            }
        }
        transactionSummaryRepository.saveAll(summaries);
    }

    // codef에서 새 거래 가져올 때마다 이번 달 summary 갱신
    @Transactional
    public void updateCurrentMonthSummary(Long userId, List<Transaction> newTransactions) {
        if (newTransactions.isEmpty()) return;

        User user = userRepository.findById(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        YearMonth currentMonth = YearMonth.now();

        // 이번 달 summary 조회
        List<TransactionSummary> existingSummaries =
            transactionSummaryRepository.findByUser_UserIdAndSummaryMonth(userId, currentMonth.toString());

        Map<TransactionCategory, TransactionSummary> summaryMap = existingSummaries.stream()
            .collect(Collectors.toMap(TransactionSummary::getTransactionCategory, s -> s));

        // 새 거래를 카테고리별로 그룹화
        Map<TransactionCategory, List<Transaction>> grouped = newTransactions.stream()
            .collect(Collectors.groupingBy(Transaction::getTransactionCategory));

        List<TransactionSummary> updates = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            TransactionCategory category = entry.getKey();
            List<Transaction> txs = entry.getValue();

            int newTotal = txs.stream().mapToInt(Transaction::getUsedAmount).sum();
            int newCount = txs.size();

            if (summaryMap.containsKey(category)) {
                // 기존 summary 업데이트
                TransactionSummary summary = summaryMap.get(category);
                summary = TransactionSummary.of(
                        summary.getTransaction_summary_id(),
                        user,
                        category,
                        currentMonth.toString(),
                        summary.getTotalAmount() + newTotal,
                        summary.getTransactionCount() + newCount,
                        LocalDate.now()
                );
                updates.add(summary);
            } else {
                // 이번 달에 처음 생긴 카테고리
                updates.add(TransactionSummary.of(
                        null,
                        user,
                        category,
                        currentMonth.toString(),
                        newTotal,
                        newCount,
                        LocalDate.now()
                ));
            }
        }
        transactionSummaryRepository.saveAll(updates);
    }

    @Transactional(readOnly = true)
    public CategorySummaryDTO getMonthlyCategorySummary(
        Long userId,
        String month,
        String category
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));

        TransactionCategory transactionCategory = Arrays.stream(TransactionCategory.values())
            .filter(c -> c.name().equalsIgnoreCase(category))
            .findFirst()
            .orElseThrow(() -> MoneyjException.of(TransactionErrorCode.INVALID_CATEGORY));

        return transactionSummaryRepository
            .findByUser_UserIdAndSummaryMonthAndTransactionCategory(user.getUserId(), month, transactionCategory)
            .map(CategorySummaryDTO::from)
            .orElseThrow(() -> MoneyjException.of(TransactionSummaryErrorCode.NOT_FOUND));
    }
}