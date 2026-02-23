package com.project.moneyj.card.service;

import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.dto.*;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.card.service.external.CardProvider;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CardErrorCode;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardProvider cardProvider;

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    // 카드 목록 조회 및 기관 연결
    @Transactional
    public List<CardInfoDTO> connectInstitutionAndFetchCards(Long userId, CardConnectionRequestDTO request) {

        // 기관 연결
        cardProvider.connectInstitution(userId, request);

        // 카드 정보 조회 (CODEF API 호출)
        List<ExternalCardDTO> externalCards = cardProvider.fetchCards(userId, request.organization());


        // CODEF에서 받아온 카드 정보를 응답 DTO로 변환하여 반환
        List<CardInfoDTO> result = externalCards.stream()
                .map(card -> CardInfoDTO.builder()
                        .cardName(card.cardName())
                        .cardNo(card.cardNo())
                        .organizationCode(card.organizationCode())
                        .build())
                .collect(Collectors.toList());

        return result;
    }

    // DB에 카드 저장
    @Transactional
    public CardResponseDTO linkCard(Long userId, CardLinkRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> MoneyjException.of(CardErrorCode.USER_NOT_FOUND));

        if (cardRepository.findByCardNo(request.getCardNo()).isPresent()) {
            throw MoneyjException.of(CardErrorCode.CARD_ALREADY_EXISTS);
        }

        Card card = Card.of(user,
                request.getCardNo(),
                request.getOrganizationCode(),
                request.getCardName()
        );

        Card savedCard = cardRepository.save(card);

        return CardResponseDTO.from(savedCard);
    }

    // 카드 변경
    @Transactional
    public CardResponseDTO switchCard(Long userId, Long cardId, CardSwitchRequestDTO request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> MoneyjException.of(CardErrorCode.CARD_NOT_FOUND));

        if (!card.getUser().getUserId().equals(userId)) {
            throw MoneyjException.of(CardErrorCode.ACCESS_DENIED);
        }

        Optional<Card> existingCardWithNewNumber = cardRepository.findByCardNo(request.getCardNo());
        if( existingCardWithNewNumber.isPresent() && !existingCardWithNewNumber.get().getCardId().equals(cardId)) {
            throw MoneyjException.of(CardErrorCode.CARD_ALREADY_EXISTS);
        }

        card.switchCardNumber(request.getCardNo(),
                request.getOrganizationCode(),
                request.getCardName()
        );

        return CardResponseDTO.from(card);
    }

    // 카드 삭제
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> MoneyjException.of(CardErrorCode.CARD_NOT_FOUND));

        if (!card.getUser().getUserId().equals(userId)) {
            throw MoneyjException.of(CardErrorCode.ACCESS_DENIED);
        }

        // 로컬 삭제
        cardRepository.delete(card);
    }
}
