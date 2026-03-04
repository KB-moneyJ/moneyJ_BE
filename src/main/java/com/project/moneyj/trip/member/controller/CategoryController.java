package com.project.moneyj.trip.member.controller;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.trip.member.dto.category.CategoryDTO;
import com.project.moneyj.trip.member.dto.category.CategoryListRequestDTO;
import com.project.moneyj.trip.member.dto.category.CategoryResponseDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedRequestDTO;
import com.project.moneyj.trip.member.dto.category.isConsumedResponseDTO;
import com.project.moneyj.trip.member.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trip-plans")
public class CategoryController  implements CategoryControllerApiSpec{

    private final CategoryService categoryService;

    /**
     * 여행 플랜 카테고리별 목표 달성 여부 변경
     */
    @Override
    @PostMapping("/isconsumed")
    public ResponseEntity<isConsumedResponseDTO> switchIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody isConsumedRequestDTO request
    ) {
        Long userId = customUser.getUserId();

        return ResponseEntity.ok(categoryService.switchIsConsumed(request, userId));
    }

    /**
     * 여행 플랜 카테고리 조회
     */
    @Override
    @GetMapping("/isconsumed/{planId}")
    public ResponseEntity<List<CategoryDTO>> getIsConsumed(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @PathVariable Long planId
    ) {
        Long userId = customUser.getUserId();

        return ResponseEntity.ok(categoryService.getIsConsumed(planId, userId));
    }

    /**
     * 여행 플랜 카테고리 변경
     */
    @Override
    @PatchMapping("/category")
    public ResponseEntity<CategoryResponseDTO> patchCategory(
        @AuthenticationPrincipal CustomOAuth2User customUser,
        @RequestBody CategoryListRequestDTO request
    ) {
        Long userId = customUser.getUserId();

        return ResponseEntity.ok(categoryService.patchCategory(request, userId));
    }


}
