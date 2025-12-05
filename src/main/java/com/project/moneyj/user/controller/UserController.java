package com.project.moneyj.user.controller;

import com.project.moneyj.user.dto.UserCheckRequestDTO;
import com.project.moneyj.user.dto.UserCheckResponseDTO;
import com.project.moneyj.user.dto.UserResponseDTO;
import com.project.moneyj.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserControllerApiSpec{

    private final UserService userService;

//    @PreAuthorize("hasRole('USER')")
    @Override
    @GetMapping()
    public ResponseEntity<UserResponseDTO> getUser() {
        UserResponseDTO userResponseDTO = userService.getUser();

        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    @PostMapping("/check")
    public ResponseEntity<List<UserCheckResponseDTO>> checkUserByEmail(
        @RequestBody UserCheckRequestDTO request
    ) {
        List<UserCheckResponseDTO> response = userService.existsByEmail(request);

        return ResponseEntity.ok(response);
    }
}
