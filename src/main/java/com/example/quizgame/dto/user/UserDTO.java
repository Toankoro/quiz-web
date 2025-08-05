package com.example.quizgame.dto.user;

import com.example.quizgame.entity.User;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String firstname;
    private String email;

    public static UserDTO fromUserToUserDTO (User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstname(user.getFirstname())
                .email(user.getEmail())
                .build();
    }

}
