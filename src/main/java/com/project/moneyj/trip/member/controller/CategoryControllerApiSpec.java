package com.project.moneyj.trip.member.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.member.dto.category.CategoryDTO;
import com.project.moneyj.trip.member.dto.category.CategoryListRequestDTO;
import com.project.moneyj.trip.member.dto.category.CategoryResponseDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedRequestDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Category", description = "경비 카테고리 API")
public interface CategoryControllerApiSpec {

    @Operation(summary = "카테고리 목표 달성 여부 변경", description = "특정 카테고리의 소비 완료 여부를 토글합니다.")
    ResponseEntity<isConsumedResponseDTO> switchIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody isConsumedRequestDTO request
    );

    @Operation(summary = "카테고리별 달성 여부 조회", description = "플랜의 모든 카테고리별 소비 완료 여부를 조회합니다.")
    ResponseEntity<List<CategoryDTO>> getIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @Parameter(description = "조회할 플랜 ID") @PathVariable Long planId
    );

    @Operation(summary = "카테고리 변경", description = "여행 플랜의 카테고리 목록을 수정합니다.")
    ResponseEntity<CategoryResponseDTO> patchCategory(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody CategoryListRequestDTO request
    );

}
