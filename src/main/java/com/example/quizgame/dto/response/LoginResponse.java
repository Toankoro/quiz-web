package com.example.quizgame.dto.response;

import com.example.quizgame.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String username;
    private String email;
    private String firstname;
    private String token;

    public static LoginResponse fromUserToLoginResponse (User user, String token) {
        return LoginResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .token(token)
                .build();
    }
}
