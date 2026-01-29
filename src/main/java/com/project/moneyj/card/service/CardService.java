package com.project.moneyj.card.service;

import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.dto.CardInfoDTO;
import com.project.moneyj.card.dto.CardLinkRequestDTO;
import com.project.moneyj.card.dto.CardResponseDTO;
import com.project.moneyj.card.dto.CardSwitchRequestDTO;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.codef.service.CodefProvider;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CardErrorCode;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CodefProvider codefProvider;
    private final CodefCardService codefCardService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;

    // 카드 목록 조회 (단순 조회용)
    @Transactional(readOnly = true)
    public List<CardInfoDTO> getCardList(Long userId, String organization) {

        Map<String, Object> codefResponse = codefCardService.fetchCards(userId, organization);

        Map<String, Object> data = (Map<String, Object>) codefResponse.get("data");
        if (data == null || data.get("resCardList") == null) {
            return List.of();
        }
        List<Map<String, Object>> cardListFromApi = (List<Map<String, Object>>) data.get("resCardList");

        return cardListFromApi.stream()
                .map(card -> CardInfoDTO.builder()
                        .cardName((String) card.get("resCardName"))
                        .cardNo((String) card.get("resCardNo"))
                        .organizationCode((String) card.get("organization"))
                        .build())
                .collect(Collectors.toList());
    }

    // 카드 목록 조회 및 기관 연결
    @Transactional
    public List<CardInfoDTO> connectInstitutionAndFetchCards(Long userId, CredentialCreateRequestDTO.CredentialInput input) {
        Optional<CodefConnectedId> existingCid = codefConnectedIdRepository.findByUserId(userId);

        if (existingCid.isEmpty()) {
            codefProvider.createConnectedId(userId, input);
        } else {
            codefProvider.addCredential(userId, input);
        }

        Map<String, Object> codefResponse = codefCardService.fetchCards(userId, input.getOrganization());

        Map<String, Object> data = (Map<String, Object>) codefResponse.get("data");
        if (data == null || data.get("resCardList") == null) {
            return List.of();
        }
        List<Map<String, Object>> cardListFromApi = (List<Map<String, Object>>) data.get("resCardList");


        return cardListFromApi.stream()
                .map(card -> CardInfoDTO.builder()
                        .cardName((String) card.get("resCardName"))
                        .cardNo((String) card.get("resCardNo"))
                        .organizationCode((String) card.get("organization"))
                        .build())
                .collect(Collectors.toList());
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
}
