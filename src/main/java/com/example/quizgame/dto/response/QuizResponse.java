package com.example.quizgame.dto.response;

import com.example.quizgame.dto.user.UserDTO;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Long id;
    private String topic;
    private boolean visibleTo;
    private List<QuestionResponse> questions;
    private Boolean favorite;
    private UserDTO createdBy;
    private LocalDateTime createdAt;
    public QuizResponse(Quiz quiz) {
        this.id = quiz.getId();
        this.topic = quiz.getTopic();
        this.visibleTo = quiz.isVisibleTo();
        this.questions = quiz.getQuestions().stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .collect(Collectors.toList());
        this.favorite = false;
    }

    public QuizResponse(Long id, String topic, List<QuestionResponse> questions) {
        this.id = id;
        this.topic = topic;
        this.questions = questions;
        this.visibleTo = isVisibleTo();
        this.favorite = false;
    }
}
