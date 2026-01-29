package com.project.moneyj.account.service;

import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.dto.AccountInfoDTO;
import com.project.moneyj.account.dto.AccountLinkRequestDTO;
import com.project.moneyj.account.dto.AccountResponseDTO;
import com.project.moneyj.account.dto.AccountSwitchRequestDTO;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.service.CodefBankService;
import com.project.moneyj.codef.service.CodefProvider;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.AccountErrorCode;
import com.project.moneyj.exception.code.TripPlanErrorCode;
import com.project.moneyj.exception.code.UserErrorCode;
import com.project.moneyj.trip.plan.domain.TripPlan;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CodefBankService codefBankService;
    private final TripPlanRepository tripPlanRepository;
    private final CodefProvider codefProvider;
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    // 등록된 계좌 목록 조회 (단순 조회용)
    @Transactional(readOnly = true)
    public List<AccountInfoDTO> getAccountList(Long userId, String organization) {

        Map<String, Object> codefResponse = codefBankService.fetchBankAccounts(userId, organization);

        Map<String, Object> data = (Map<String, Object>) codefResponse.get("data");
        if (data == null || data.get("resDepositTrust") == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> depositAccounts = (List<Map<String, Object>>) data.get("resDepositTrust");

        return depositAccounts.stream()
            .map(acc -> AccountInfoDTO.builder()
                .organizationCode((String) acc.get("organization"))
                .accountName((String) acc.get("resAccountName"))
                .accountNumber((String) acc.get("resAccount"))
                .balance(Integer.parseInt(String.valueOf(acc.get("resAccountBalance"))))
                .build())
            .collect(Collectors.toList());
    }

    // 기관 연결 및 등록된 계좌 목록 조회
    @Transactional
    public List<AccountInfoDTO> connectInstitutionAndFetchAccounts(Long userId, CredentialCreateRequestDTO.CredentialInput input) {

        // 기관 등록
        Optional<CodefConnectedId> existingCid = codefConnectedIdRepository.findByUserId(userId);
        if (existingCid.isEmpty()) {
            // connectedId가 없다면 -> 최초 연동
            codefProvider.createConnectedId(userId, input);
        } else {
            // connectedId가 있다면 -> 기관 추가
            codefProvider.addCredential(userId, input);
        }

        // 등록된 계좌 목록 조회
        Map<String, Object> codefResponse = codefBankService.fetchBankAccounts(userId, input.getOrganization());

        Map<String, Object> data = (Map<String, Object>) codefResponse.get("data");
        if (data == null || data.get("resDepositTrust") == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> depositAccounts = (List<Map<String, Object>>) data.get("resDepositTrust");

        return depositAccounts.stream()
            .map(acc -> AccountInfoDTO.builder()
                .organizationCode((String) acc.get("organization"))
                .accountName((String) acc.get("resAccountName"))
                .accountNumber((String) acc.get("resAccount"))
                .balance(Integer.parseInt(String.valueOf(acc.get("resAccountBalance"))))
                .build())
            .collect(Collectors.toList());
    }

    // 사용자가 선택한 은행 계좌를 저장
    @Transactional
    public AccountResponseDTO linkUserAccount(Long userId, AccountLinkRequestDTO request) {

        // 이 여행 계획에 이미 연결된 계좌가 있는지 확인
        if (accountRepository.findByUserIdAndTripPlanId(userId, request.getTripPlanId()).isPresent()) {
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_ALREADY_IN_USE);
        }

        // 이 계좌번호가 다른 여행에서 이미 사용 중인지 확인
        if (accountRepository.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_ALREADY_IN_USE);
        }

        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> MoneyjException.of(UserErrorCode.NOT_FOUND));
        TripPlan tripPlan = tripPlanRepository.findById(request.getTripPlanId())
            .orElseThrow(() -> MoneyjException.of(TripPlanErrorCode.NOT_FOUND));

        Account newAccount = Account.of(
            user,
            tripPlan,
            request.getAccountNumber(),
            maskAdvanced(request.getAccountNumber()),
            request.getBalance(),
            request.getOrganizationCode(),
            request.getAccountName()
        );

        // DB에 저장
        try {
            accountRepository.save(newAccount);
        } catch (DataIntegrityViolationException e) {
            log.warn("계좌 저장 중 데이터 무결성 오류 발생: accountNumber={}", request.getAccountNumber());
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_ALREADY_IN_USE);
        }

        return AccountResponseDTO.builder()
            .accountId(newAccount.getAccountId())
            .accountName(newAccount.getAccountName())
            .accountNumber(maskAdvanced(newAccount.getAccountNumber()))
            .balance(newAccount.getBalance())
            .build();
    }

    // 계좌의 마지막 업데이트가 3시간 이후일 경우에만 CODEF를 호출해 해당 계좌 잔액을 갱신.
    @Transactional
    public void syncAccountIfNeeded(Account account) {

        Long userId = account.getUser().getUserId();
        String orgCode = account.getOrganizationCode();
        String accountNumber = account.getAccountNumber();

        if (orgCode == null || accountNumber == null) {
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }

        // CODEF API 호출
        Map<String, Object> res = codefBankService.fetchBankAccounts(userId, orgCode);

        Map<String, Object> data = (Map<String, Object>) res.get("data");
        if (data == null || data.get("resDepositTrust") == null) {
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }

        List<Map<String, Object>> accountsFromCodef = (List<Map<String, Object>>) data.get("resDepositTrust");

        // 현재 Account와 매칭되는 CODEF 계좌 찾아서 업데이트
        accountsFromCodef.stream()
                .filter(m -> accountNumber.equals(String.valueOf(m.get("resAccount"))))
                .findFirst()
                .ifPresent(map -> {

                    long balanceLong = Long.parseLong(String.valueOf(map.get("resAccountBalance")));
                    account.updateBalance((int) balanceLong);
                });
    }

    // 계좌 수동 업데이트
    @Transactional
    public AccountResponseDTO manualAccount(Long accId) {
        Account account = accountRepository.findById(accId)
                .orElseThrow(() -> MoneyjException.of(AccountErrorCode.ACCOUNT_NOT_FOUND));

        syncAccountIfNeeded(account);

        return AccountResponseDTO.builder()
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .accountNumber(maskAdvanced(account.getAccountNumber()))
                .balance(account.getBalance())
                .build();
    }

    // 계좌 변경
    @Transactional
    public AccountResponseDTO switchAccount(Long userId, Long accountId, AccountSwitchRequestDTO requestDTO){

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> MoneyjException.of(AccountErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getUserId().equals(userId)) {
            throw MoneyjException.of(AccountErrorCode.ACCESS_DENIED);
        }

        // 다른 여행에 해당 계좌가 사용 중인지 확인
        Optional<Account> existingAccountWithNewNumber = accountRepository.findByAccountNumber(requestDTO.getAccountNumber());
        if (existingAccountWithNewNumber.isPresent() && !existingAccountWithNewNumber.get().getAccountId().equals(accountId)){
            // DB에 해당 계좌번호가 존재하고, 그게 지금 변경하려는 계좌가 아니라면 중복 사용입니다.
            throw MoneyjException.of(AccountErrorCode.ACCOUNT_ALREADY_IN_USE);
        }

        account.switchAccountNumber(
                requestDTO.getAccountNumber(),
                maskAdvanced(requestDTO.getAccountNumber()),
                requestDTO.getBalance(),
                requestDTO.getOrganizationCode(),
                requestDTO.getAccountName());

        return AccountResponseDTO.builder()
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .accountNumber(maskAdvanced(account.getAccountNumber()))
                .balance(account.getBalance())
                .build();
    }

    // "1234-****-5678" 형태로 마스킹
    public static String maskAdvanced(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber; // 너무 짧으면 마스킹하지 않음
        }
        String firstPart = accountNumber.substring(0, 4);
        String lastPart = accountNumber.substring(accountNumber.length() - 4);
        return firstPart + "****" + lastPart;
    }

    @Transactional(readOnly = true)
    public Integer getUserBalance(Long userId, Long planId) {
        return accountRepository.findByUserIdAndTripPlanId(userId, planId)
                .map(Account::getBalance)
                .orElseThrow(() -> MoneyjException.of(AccountErrorCode.USER_ACCOUNT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean checkAccountOwnership(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).isPresent();
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> MoneyjException.of(AccountErrorCode.NOT_FOUND));

        accountRepository.delete(account);
    }
}