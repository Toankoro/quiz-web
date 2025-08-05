package com.example.quizgame.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FavoriteQuizResponse {
    private Long id;
    private Long quizId;
    private String quizTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime addedAt;
    private List<QuestionResponse> questions;

    public FavoriteQuizResponse() {
    }

    public FavoriteQuizResponse(Long id, Long quizId, String quizTitle, LocalDateTime addedAt,
            List<QuestionResponse> questions) {
        this.id = id;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.addedAt = addedAt;
        this.questions = questions;
    }
}