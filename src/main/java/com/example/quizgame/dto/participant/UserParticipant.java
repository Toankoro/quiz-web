package com.example.quizgame.dto.participant;

import com.example.quizgame.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserParticipant {
    private String username;
//    private String clientSessionId;
    private String pinCode;

    public static UserParticipant fromUserToUserParticipant (User user, String pinCode) {
        return UserParticipant.builder()
                .username(user.getUsername())
//                .clientSessionId(clientSessionId)
                .pinCode(pinCode).build();
    }
}