package com.example.quizgame.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteQuizResponse {
    private Long id;
    private Long quizId;
    private String quizTitle;
    private String quizName;
    private String quizDescription;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime addedAt;
    private String quizImageUrl;
    private String creatorName;
    private String creatorImageUrl;

}