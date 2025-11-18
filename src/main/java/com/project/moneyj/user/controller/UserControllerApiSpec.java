package com.project.moneyj.user.controller;

import com.project.moneyj.user.dto.UserCheckRequestDTO;
import com.project.moneyj.user.dto.UserCheckResponseDTO;
import com.project.moneyj.user.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserControllerApiSpec {

    @Operation(summary = "유저 정보 조회", description = "사용자의 정보를 반환합니다.")
    ResponseEntity<UserResponseDTO> getUser();

    @Operation(summary = "유저 존재 여부 확인", description = "이메일로 사용자의 존재 여부를 판단하여 정보를 반환합니다.")
    ResponseEntity<List<UserCheckResponseDTO>> checkUserByEmail(
        @RequestBody UserCheckRequestDTO request
    );

}
