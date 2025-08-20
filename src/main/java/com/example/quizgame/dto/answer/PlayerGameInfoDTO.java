package com.example.quizgame.dto.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerGameInfoDTO {
    private String topicName;       // từ Quiz/Room
    private String quizName;        // từ Quiz
    private LocalDateTime startedAt; // thời gian bắt đầu phòng
    private String playerName;      // user.firstname
    private Integer ranking;        // từ GameRanking
    private Integer score;          // từ GameRanking
    private Integer correctCount;   // từ GameRanking
    private Integer wrongCount;     // tính từ PlayerAnswer
    private Integer totalQuestions; // tổng số câu PlayerAnswer
}