package com.example.quizgame.dto.answer;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnswerResult implements Serializable {
    private Long questionId;
    private String clientSessionId;
    private Long roomParticipantId;
    private String selectedAnswer;
    private int score;
    private boolean correct;
    private Float timeTaken;
    private String correctAnswer;
}