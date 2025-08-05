package com.example.quizgame.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String firstname;
    private String email;
    private String socialLinks;
    private String avatar;
}

