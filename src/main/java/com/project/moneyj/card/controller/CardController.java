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

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards") // Base path changed to /cards
public class CardController implements CardControllerApiSpec {

    private final CardService cardService;
    private final CodefCardService codefCardService;

    /**
     * 보유 카드 목록 조회
     * 새로고침 등으로 카드 목록만 다시 불러오고 싶을 때 사용
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getCardList(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestParam String organization) {

        Long userId = customUser.getUserId();
        return ResponseEntity.ok(codefCardService.fetchCards(userId, organization));
    }

    /**
     * 카드 목록 조회 및 기관 연결
     * CODEF를 통해 기관(은행/카드사)에 연결하고, 성공 시 해당 기관의 카드 목록을 반환
     * 최초 등록시 커넥티드 ID 발급
     */
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
    @PostMapping("/link")
    public ResponseEntity<CardResponseDTO> linkCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CardLinkRequestDTO request) {
        Long userId = customUser.getUserId();
        CardResponseDTO responseDto = cardService.linkCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 카드 변경
     */
    @PatchMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> switchCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long cardId,
            @RequestBody CardSwitchRequestDTO request) {
        Long userId = customUser.getUserId();
        CardResponseDTO responseDto = cardService.switchCard(userId, cardId, request);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 카드 삭제
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long cardId) {
        Long userId = customUser.getUserId();
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.noContent().build();
    }
}
