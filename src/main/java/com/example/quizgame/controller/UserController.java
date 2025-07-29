package com.example.quizgame.controller;

import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.user.UserLevelResponse;
import com.example.quizgame.dto.user.UserProfileResponse;
import com.example.quizgame.dto.user.UserProfileUpdateRequest;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepo;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getProfile(username));
    }
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(@ModelAttribute UserProfileUpdateRequest request,
                                           Authentication authentication) {
        String username = authentication.getName();
        try {
            userService.updateProfile(username, request);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace(); // 👈 log chi tiết lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Avatar upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/level")
    public ResponseEntity<?> getLevel(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepo.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        UserLevelResponse response = new UserLevelResponse(user.getLevel(), user.getExp());
        return ResponseEntity.ok(response);
    }

}
