package com.theradio.auth;

import com.theradio.auth.dto.AuthResponse;
import com.theradio.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    public UserController(AuthService authService) {
        this.authService = authService;
    }


    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser() {
        try {
            User user = authService.getCurrentUser();
            AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .displayName(user.getDisplayName())
                    .isLive(user.getIsLive())
                    .build();
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }
}

