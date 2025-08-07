package com.example.quizgame.dto.request;

import com.example.quizgame.dto.question.QuestionRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizRequest {
    private String topic;
    private String name;
    private boolean visibleTo;
    private String imageUrl;
    private List<QuestionRequest> questions;
}
