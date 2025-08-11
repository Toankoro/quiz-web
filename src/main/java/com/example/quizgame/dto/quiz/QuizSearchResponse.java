package com.example.quizgame.dto.quiz;

import com.example.quizgame.entity.Quiz;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSearchResponse {
    private Long quizId;
    private String quizTopic;
    private String quizName;
    private String quizImageUrl;
    private String quizDescription;
    private Integer quantityQuestion;
    private boolean visibleTo;
    private String creator;
    private String creatorImageUrl;
    private LocalDateTime createdAt;

    public static QuizSearchResponse fromQuizToQuizSearchResponse (Quiz quiz) {
        return QuizSearchResponse.builder()
                .quizId(quiz.getId())
                .quizTopic(quiz.getTopic())
                .quizName(quiz.getName())
                .quizImageUrl(quiz.getImageUrl())
                .quizDescription(quiz.getDescription())
                .quantityQuestion(quiz.getQuestions().size())
                .visibleTo(quiz.isVisibleTo())
                .creator(quiz.getCreatedBy().getUsername())
                .creatorImageUrl(quiz.getCreatedBy().getAvatar())
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}



