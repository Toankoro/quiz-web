package com.example.quizgame.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionTimerResponse {
    private Long questionId;
    private Integer baseTimeLimit; // Thời gian cơ bản (10 giây)
    private Long startTime; // Thời điểm bắt đầu câu hỏi
    private String message; // Thông báo chung
}