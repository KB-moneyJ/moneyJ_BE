package com.project.moneyj.card.service;

import com.project.moneyj.card.domain.Card;
import com.project.moneyj.card.dto.CardInfoDTO;
import com.project.moneyj.card.dto.CardLinkRequestDTO;
import com.project.moneyj.card.dto.CardResponseDTO;
import com.project.moneyj.card.dto.CardSwitchRequestDTO;
import com.project.moneyj.card.repository.CardRepository;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
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

    private final CodefProvider codefProvider;

    private final CodefCardService codefCardService;

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CodefConnectedIdRepository codefConnectedIdRepository;
    private final CodefInstitutionRepository codefInstitutionRepository;

    // 카드 목록 조회 및 기관 연결
    @Transactional
    public List<CardInfoDTO> connectInstitutionAndFetchCards(Long userId, CredentialCreateRequestDTO.CredentialInput input) {

        Optional<CodefConnectedId> existingCid = codefConnectedIdRepository.findByUserId(userId);

        // 기관 등록
        if (existingCid.isEmpty()) {
            codefProvider.createConnectedId(userId, input);
        } else {
            String cid = existingCid.get().getConnectedId();
            Optional<CodefInstitution> existingInstitution = codefInstitutionRepository
                    .findByConnectedIdAndOrganization(cid, input.getOrganization());

            if(existingInstitution.isEmpty()) {
                // connectedId가 있고, 기관 등록이 안되었다면 -> 기관 추가
                // connectedId가 있고, 기관 등록이 이미 되어있다면 -> 카드 조회로 바로 진행
                codefProvider.addCredential(userId, input);
            }
        }

        Map<String, Object> codefResponse = codefCardService.fetchCards(userId, input.getOrganization());

        Object dataObj = codefResponse.get("data");
        List<Map<String, Object>> cardListFromApi = normalizeToList(dataObj);

        return cardListFromApi.stream()
                .map(card -> CardInfoDTO.builder()
                        .cardName((String) card.get("resCardName"))
                        .cardNo((String) card.get("resCardNo"))
                        .organizationCode(input.getOrganization())
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeToList(Object obj) {
        if (obj == null) return List.of();

        if (obj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        if (obj instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }

        return List.of();
    }

}
