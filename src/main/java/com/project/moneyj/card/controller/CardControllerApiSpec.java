package com.project.moneyj.card.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.card.dto.CardInfoDTO;
import com.project.moneyj.card.dto.CardLinkRequestDTO;
import com.project.moneyj.card.dto.CardResponseDTO;
import com.project.moneyj.card.dto.CardSwitchRequestDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "Cards", description = "카드 API")
public interface CardControllerApiSpec {

    @Operation(summary = "카드사 연결 및 카드 목록 조회", description = "CODEF를 통해 카드사에 연결하고, 성공 시 해당 기관의 카드 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기관 연결 및 카드 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 CODEF 비즈니스 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<List<CardInfoDTO>> connectInstitutionAndFetchCards(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CredentialCreateRequestDTO.CredentialInput input);

    @Operation(summary = "선택한 카드를 DB에 저장 (연결)", description = "사용자가 CODEF에서 조회한 카드 목록 중 선택한 카드를 로컬 DB에 저장(연결)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "카드 저장(연결) 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 존재하는 카드"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<CardResponseDTO> linkCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestBody CardLinkRequestDTO request);

    @Operation(summary = "카드 변경", description = "DB에 저장된 카드를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카드 정보 변경 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    })
    ResponseEntity<CardResponseDTO> switchCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @Parameter(description = "변경할 카드의 ID", required = true) @PathVariable Long cardId,
            @RequestBody @Valid CardSwitchRequestDTO request);

    @Operation(summary = "카드 삭제", description = "DB에 저장된 카드를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "카드 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    })
    ResponseEntity<String> deleteCard(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @Parameter(description = "삭제할 카드의 ID", required = true) @PathVariable Long cardId);
}
