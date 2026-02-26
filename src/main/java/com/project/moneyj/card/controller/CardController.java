package com.project.moneyj.card.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.card.dto.CardInfoDTO;
import com.project.moneyj.card.dto.CardLinkRequestDTO;
import com.project.moneyj.card.dto.CardResponseDTO;
import com.project.moneyj.card.dto.CardSwitchRequestDTO;
import com.project.moneyj.card.service.CardService;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.service.CodefCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardController implements CardControllerApiSpec {

    private final CardService cardService;

    /**
     * 카드 목록 조회 및 기관 연결
     * CODEF를 통해 기관(은행/카드사)에 연결하고, 성공 시 해당 기관의 카드 목록을 반환
     * 최초 등록시 커넥티드 ID 발급
     * '카드 변경' 시에도 사용
     */
    @Override
    @PostMapping("/connect")
    public ResponseEntity<List<CardInfoDTO>> connectInstitutionAndFetchCards(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CredentialCreateRequestDTO.CredentialInput input) {
        Long userId = customUser.getUserId();
        List<CardInfoDTO> cards = cardService.connectInstitutionAndFetchCards(userId, input);
        return ResponseEntity.ok(cards);
    }

    /**
     * 사용자가 선택한 카드를 DB에 저장
     */
    @Override
    @PostMapping("/link")
    public ResponseEntity<CardResponseDTO> linkCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody @Valid CardLinkRequestDTO request) {
        Long userId = customUser.getUserId();
        CardResponseDTO responseDto = cardService.linkCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 카드 변경
     */
    @Override
    @PatchMapping("/switch/{cardId}")
    public ResponseEntity<CardResponseDTO> switchCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long cardId,
            @RequestBody @Valid CardSwitchRequestDTO request) {
        Long userId = customUser.getUserId();
        CardResponseDTO responseDto = cardService.switchCard(userId, cardId, request);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 등록된 카드 정보 조회
     */
    @Override
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> getCardInfo(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long cardId) {
        Long userId = customUser.getUserId();
        return ResponseEntity.ok(cardService.getCardInfo(userId, cardId));
    }

    /**
     * 카드 삭제
     */
    @Override
    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> deleteCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long cardId) {
        Long userId = customUser.getUserId();
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.ok("카드가 삭제되었습니다.");
    }
}
