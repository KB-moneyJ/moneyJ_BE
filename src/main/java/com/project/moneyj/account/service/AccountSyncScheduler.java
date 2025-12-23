package com.project.moneyj.account.service;

import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.codef.service.CodefBankService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSyncScheduler {

    private final AccountRepository accountRepository;
    private final CodefBankService codefBankService;

    /**
     * 매일 00시, 06시, 12시, 18시에 전체 계좌 스냅샷 동기화
     */
    @Transactional
    @Scheduled(cron = "0 0 0,6,12,18 * * *")
    //@Scheduled(cron = "0 */1 * * * *")
    public void syncAllAccountsSnapshot() {
        log.info("[Scheduler] 계좌 정기 동기화 시작");

        List<Account> accounts = accountRepository.findAll();
        if (accounts.isEmpty()) {
            return;
        }

        // (최소화용) userId + orgCode 기준으로 CODEF 호출 결과 캐싱
        Map<String, List<Map<String, Object>>> cache = new HashMap<>();

        for (Account account : accounts) {
            Long userId = account.getUser().getUserId();
            String orgCode = account.getOrganizationCode();
            String accountNumber = account.getAccountNumber();

            if (orgCode == null || accountNumber == null) {
                continue;
            }

            String key = userId + "|" + orgCode;

            List<Map<String, Object>> depositAccounts = cache.computeIfAbsent(key, k -> {
                Map<String, Object> res = codefBankService.fetchBankAccounts(userId, orgCode);
                Map<String, Object> data = (Map<String, Object>) res.get("data");
                if (data == null || data.get("resDepositTrust") == null) {
                    log.warn("정기 동기화 실패: userId={}, orgCode={} (응답에 계좌 없음)", userId, orgCode);
                    return Collections.emptyList();
                }
                return (List<Map<String, Object>>) data.get("resDepositTrust");
            });

            if (depositAccounts.isEmpty()) {
                continue;
            }

            Optional<Map<String, Object>> match = depositAccounts.stream()
                    .filter(acc -> accountNumber.equals(String.valueOf(acc.get("resAccount"))))
                    .findFirst();

            if (match.isPresent()) {
                Integer latestBalance = Integer.parseInt(String.valueOf(match.get().get("resAccountBalance")));
                account.updateBalance(latestBalance);
                log.debug("정기 동기화: userId={}, account={}, balance={}",
                        userId, accountNumber, latestBalance);
            } else {
                log.warn("정기 동기화: userId={}, account={} 매칭 실패", userId, accountNumber);
            }
        }

        log.info("정기 계좌 동기화 완료 (총 {}건)", accounts.size());
    }

}
