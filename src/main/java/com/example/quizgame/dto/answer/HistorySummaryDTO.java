package com.example.quizgame.dto.answer;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistorySummaryDTO {
    private Long roomId;
    private String quizTitle;
    private String lessonName;
    private int questionCount;
    private int totalScore;
    private LocalDateTime playedAt;
}
