package com.example.quizgame.dto.answer;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PlayHistoryDTO {
    private String topic;
    private String quizName;
    private long quizId;
    private long roomId;
    private long totalQuestions;
    private LocalDateTime startedAt;
}
