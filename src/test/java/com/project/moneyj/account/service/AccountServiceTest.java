package com.project.moneyj.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.project.moneyj.account.domain.Account;
import com.project.moneyj.account.repository.AccountRepository;
import com.project.moneyj.codef.service.CodefBankService;
import com.project.moneyj.trip.plan.repository.TripPlanRepository;
import com.project.moneyj.user.domain.Role;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private CodefBankService codefBankService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TripPlanRepository tripPlanRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

//    @Test
//    @DisplayName("기존 계좌가 없다면 CODEF 조회 후 새 계좌를 생성")
//    void linkUserAccountTest1(){
//        //given
//        Long userId = 1L;
//        Long tripPlanId = 10L;
//
//        AccountLinkRequestDTO request = AccountLinkRequestDTO.builder()
//                .tripPlanId(tripPlanId)
//                .accountNumber("1234-5678-9012")
//                .organizationCode("001")
//                .build();
//
//        User user = User.of("test", "test@test.com", "pw", Role.ROLE_USER);
//
//        TripPlan tripPlan = TripPlan.of(
//                3, "France", "FR", "Paris", 5, 4,
//                LocalDate.of(2026, 5, 10),
//                LocalDate.of(2026, 5, 14),
//                3000000,
//                LocalDate.of(2025, 12, 1),
//                LocalDate.of(2026, 4, 30)
//        );
//
//        //기존 계좌가 없음
//        given(accountRepository.findByAccountNumber(request.getAccountNumber()))
//                .willReturn(Optional.empty());
//
//        //CODEF 응답
//        Map<String, Object> accountMap = Map.of(
//                "resAccount", "1234-5678-9012",
//                "resAccountBalance", "5000",
//                "resAccountName", "테스트계좌"
//        );
//
//        Map<String, Object> data = Map.of(
//                "resDepositTrust", List.of(accountMap)
//        );
//
//        Map<String, Object> codefResponse = Map.of(
//                "data", data
//        );
//
//        //외부 API Mock 동작 정의
//        //codefBankService.fetchBankAccounts를 호출하면 codefResponse를 반환함
//        given(codefBankService.fetchBankAccounts(userId, "001"))
//                .willReturn(codefResponse);
//
//        given(userRepository.findByUserId(userId))
//                .willReturn(Optional.of(user));
//
//        given(tripPlanRepository.findById(tripPlanId))
//                .willReturn(Optional.of(tripPlan));
//
//        //어떤 Account 객체가 오든 상관없이 동작을 실행
//        //저장(save) 시점의 동작 후 Id값 세팅
//        given(accountRepository.save(any(Account.class)))
//                .willAnswer(inv -> {
//                    Account saved = inv.getArgument(0);
//                    ReflectionTestUtils.setField(saved, "accountId", 100L);
//                    return saved;
//                });
//        //when
//        AccountResponseDTO response = accountService.linkUserAccount(userId, request);
//
//        //then
//        //저장된 계좌 ID가 설정한 ID 값인지
//        assertEquals(100L, response.getAccountId());
//
//        //CODEF 응답의 계좌명이 DTO에 잘 들어갔느지
//        assertEquals("테스트계좌", response.getAccountName());
//
//        //CODEF 응답의 잔액이 반영됐는지
//        assertEquals(5000, response.getBalance());
//
//        //각각의 메서드가 1번 실행됐는지 검증
//        verify(codefBankService, times(1)).fetchBankAccounts(userId, "001");
//        verify(accountRepository, times(1)).save(any(Account.class));
//    }
//
//    @Test
//    @DisplayName("기존 계좌가 있고 계좌 번호가 같으면 CODEF 조회 후 잔액만 갱신")
//    void linkUserAccountTest2(){
//
//        Long userId = 1L;
//        Long tripPlanId = 10L;
//
//        AccountLinkRequestDTO request = AccountLinkRequestDTO.builder()
//                .tripPlanId(tripPlanId)
//                .accountNumber("1234-5678-9012")
//                .organizationCode("001")
//                .build();
//
//        User user = User.of("test", "test@test.com", "pw", Role.ROLE_USER);
//
//        //계좌 세팅
//        Account existingAccount = Account.of(
//                user,
//                null,
//                "1234-5678-9012",
//                "1234****9012",
//                1000,
//                "001",
//                "기존계좌"
//        );
//
//        ReflectionTestUtils.setField(user, "userId", userId);
//        ReflectionTestUtils.setField(existingAccount, "accountId", 100L);
//
//        given(accountRepository.findByAccountNumber("1234-5678-9012"))
//                .willReturn(Optional.of(existingAccount));
//
//        // CODEF 응답: 같은 계좌번호를 가진 항목이 있어야 findAccountInList 통과
//        Map<String, Object> accountMap = Map.of(
//                "resAccount", "1234-5678-9012",
//                "resAccountBalance", "5000",
//                "resAccountName", "기존계좌"
//        );
//        Map<String, Object> data = Map.of("resDepositTrust", List.of(accountMap));
//        Map<String, Object> codefResponse = Map.of("data", data);
//
//        given(codefBankService.fetchBankAccounts(userId, "001"))
//                .willReturn(codefResponse);
//
//        // when
//        AccountResponseDTO response = accountService.linkUserAccount(userId, request);
//
//        // then
//        // 잔액이 갱신됐는지
//        assertEquals(5000, existingAccount.getBalance());
//        assertEquals(5000, response.getBalance());
//
//        verify(codefBankService, times(1)).fetchBankAccounts(userId, "001");
//        verify(accountRepository, never()).save(any(Account.class));
//    }

    @Test
    @DisplayName("CODEF 응답에서 매칭되는 계좌가 있으면 잔액을 갱신")
    void syncAccountIfNeededTest(){
        //given
        User user = User.of("test", "test@test.com", "pw", Role.ROLE_USER);

        //객체 내부의 필드 값을 세팅
        ReflectionTestUtils.setField(user, "userId", 1L);

        Account account = Account.of(
                user,
                null, //syncAccountIfNeeded 메서드에서 사용하지 않음
                "1234",
                "1234",
                1000,
                "001",
                "테스트계좌"
        );

        //CODEF 응답 형태
        Map<String, Object> accountMap = Map.of(
                "resAccount", "1234",
                "resAccountBalance", "5000"
        );

        Map<String, Object> data = Map.of(
                "resDepositTrust", List.of(accountMap)
        );

        Map<String, Object> codefResponse = Map.of(
                "data", data
        );

        // 외부 API Mock 동작 정의
        // 테스트 중에 codefBankService.fetchBankAccounts를 호출하면 codefResponse를 반환
        given(codefBankService.fetchBankAccounts(1L, "001"))
                .willReturn(codefResponse);

        //when
        //테스트 대상 메서드를 실행
        accountService.syncAccountIfNeeded(account);

        //then
        //결과 검증 (account.getBalance의 결과는 5000이 되어야 함)
        assertEquals(5000, account.getBalance());

        //accountService가 codefBankService.fetchBankAccounts를 1번 호출했는지 검증
        verify(codefBankService, times(1)).fetchBankAccounts(1L, "001");
    }
//
//    @Test
//    @DisplayName("계좌 변경 요청 테스트")
//    void switchAccountTest(){
//        //given
//        Long userId = 1L;
//        Long accountId = 1L;
//
//        User user = User.of("user", "u@u.com", "pw", Role.ROLE_USER);
//        ReflectionTestUtils.setField(user, "userId", userId);
//
//        Account account = Account.of(
//                user,
//                null,
//                "1234-5678-9012",
//                "1234****9012",
//                1000,
//                "001",
//                "기존계좌"
//        );
//
//        ReflectionTestUtils.setField(account, "accountId", accountId);
//
//        //request 세팅
//        AccountSwitchRequestDTO req = new AccountSwitchRequestDTO();
//        ReflectionTestUtils.setField(req, "accountNumber", "9999-0000-1111");
//
//        given(accountRepository.findById(accountId))
//                .willReturn(Optional.of(account));
//
//        // when
//        AccountResponseDTO res = accountService.switchAccount(userId, accountId, req);
//
//        // then
//        assertEquals(accountId, res.getAccountId());
//        assertEquals("기존계좌", res.getAccountName());
//        assertEquals(1000, res.getBalance());
//
//        // 계좌번호가 실제로 바뀌었는지(엔티티 상태)
//        assertEquals("9999-0000-1111", account.getAccountNumber());
//
//        // 마스킹 결과가 DTO에 반영되는지
//        assertEquals(AccountService.maskAdvanced("9999-0000-1111"), res.getAccountNumber());
//
//        verify(accountRepository, times(1)).findById(accountId);
//        verify(accountRepository, never()).save(any()); //
//    }
}