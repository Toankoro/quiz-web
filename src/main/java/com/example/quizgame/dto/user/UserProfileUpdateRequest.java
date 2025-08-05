package com.example.quizgame.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserProfileUpdateRequest {
    private String firstname;
    private String email;
    private String socialLinks;
    private MultipartFile avatar;
}
