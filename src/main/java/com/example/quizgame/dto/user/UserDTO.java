package com.example.quizgame.dto.user;

import com.example.quizgame.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private boolean loginDisabled;
    private String firstname;
    private LocalDateTime createdAt;
    private String email;

    public static UserDTO fromUserToUserDTO (User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstname(user.getFirstname())
                .email(user.getEmail())
                .loginDisabled(user.isLoginDisabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

}
