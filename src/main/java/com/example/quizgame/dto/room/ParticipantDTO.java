package com.example.quizgame.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class ParticipantDTO {
    private Long id;
    private String firstname;
    private String avatar;
    private boolean isHost;
}