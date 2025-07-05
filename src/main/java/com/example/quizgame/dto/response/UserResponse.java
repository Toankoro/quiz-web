package com.example.quizgame.dto.response;

import com.example.quizgame.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String username;
    private String email;
    private String firstname;
    private String lastname;

    public static UserResponse fromUserToUserResponse (User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .build();
    }

}
