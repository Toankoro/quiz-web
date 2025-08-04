package com.example.quizgame.dto.request;

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
    private String title;
    private String name;
    private String visibleTo;
    private String imageUrl;
    private List<QuestionRequest> questions;
}
