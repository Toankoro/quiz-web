package com.example.quizgame.dto.quiz;

import com.example.quizgame.entity.FavoriteQuiz;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteQuizResponse {
    private Long id;
    private Long quizId;
    private String quizTopic;
    private String quizName;
    private String quizDescription;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime addedAt;
    private String quizImageUrl;
    private String creatorFirstName;
    private String creatorUserName;
    private String creatorImageUrl;

    public static FavoriteQuizResponse fromFavoriteQuizToFavoriteQuizResponse (FavoriteQuiz favoriteQuiz) {
        return FavoriteQuizResponse.builder()
                .id(favoriteQuiz.getId())
                .quizId(favoriteQuiz.getQuiz().getId())
                .quizTopic(favoriteQuiz.getQuiz().getTopic())
                .quizName(favoriteQuiz.getQuiz().getName())
                .quizDescription(favoriteQuiz.getQuiz().getDescription())
                .addedAt(favoriteQuiz.getCreatedAt())
                .quizImageUrl(favoriteQuiz.getQuiz().getImageUrl())
                .creatorFirstName(favoriteQuiz.getUser().getUsername())
                .creatorUserName(favoriteQuiz.getUser().getUsername())
                .creatorImageUrl(favoriteQuiz.getUser().getAvatar())
                .build();
    }

}