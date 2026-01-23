package com.project.moneyj.transaction;

import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.transaction.service.TransactionService;
import com.project.moneyj.transaction.service.TransactionSyncScheduler;
import com.project.moneyj.user.domain.Role;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionSchedulerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransactionSyncScheduler transactionSyncScheduler;

    @Test
    @DisplayName("카드가 연동된 여러 유저 목록으로 주간 거래내역 갱신을 호출한다")
    void weeklySync_should_call_updateWeeklyTransactions_for_multiple_users() {

        // given
        User user1 = User.of(
                "user1",
                "user1@test.com",
                "profile1.png",
                Role.ROLE_USER
        );
        ReflectionTestUtils.setField(user1, "userId", 1L);

        User user2 = User.of(
                "user2",
                "user2@test.com",
                "profile2.png",
                Role.ROLE_USER
        );
        ReflectionTestUtils.setField(user2, "userId", 2L);

        Card card1 = Card.of(user1, "004", "1234","국민카드");
        Card card2 = Card.of(user2, "005", "5678","신한카드");

        given(userRepository.findAllByCardConnectedTrue())
                .willReturn(List.of(user1, user2));

        given(cardRepository.findAllByUser(user1))
                .willReturn(List.of(card1));

        given(cardRepository.findAllByUser(user2))
                .willReturn(List.of(card2));

        // when
        transactionSyncScheduler.weeklySync();

        // then
        verify(transactionService, times(2))
                .updateWeeklyTransactions(
                        any(User.class),
                        any(CardApprovalRequestDTO.class)
                );
    }
}
