package com.example.quizgame.dto.response;

import com.example.quizgame.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Long id;
    private String title;
    private List<QuestionResponse> questions;
    private Boolean favorite;

    public QuizResponse(Quiz quiz) {
        this.id = quiz.getId();
        this.title = quiz.getTitle();
        this.questions = quiz.getQuestions().stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .collect(Collectors.toList());
        this.favorite = false;
    }

    public QuizResponse(Long id, String title, List<QuestionResponse> questions) {
        this.id = id;
        this.title = title;
        this.questions = questions;
        this.favorite = false; // Default value
    }
}
