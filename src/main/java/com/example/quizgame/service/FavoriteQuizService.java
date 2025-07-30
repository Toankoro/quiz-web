package com.example.quizgame.service;

import com.example.quizgame.dto.response.FavoriteQuizResponse;

import java.util.List;

public interface FavoriteQuizService {
    boolean addToFavorites(Long quizId);

    boolean removeFromFavorites(Long quizId);

    List<FavoriteQuizResponse> getUserFavorites();

    boolean isFavorite(Long quizId);

    List<FavoriteQuizResponse> getFavoritesByUserId(Long userId);
}