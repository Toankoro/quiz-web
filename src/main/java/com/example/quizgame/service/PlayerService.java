package com.example.quizgame.service;

import com.example.quizgame.dto.AnswerMessage;
import com.example.quizgame.dto.PlayerAnswer;
import com.example.quizgame.dto.response.PlayerResponse;
import com.example.quizgame.entity.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerService {
    Player registerPlayer(String playerName, String roomCode, String wsSessionId, String clientSessionId);

    void saveAnswer(AnswerMessage message);

    List<PlayerAnswer> getPlayerAnswers(Long playerId);

    List<PlayerResponse> getPlayersInRoom(String roomCode);

    Optional<String> removePlayerBySessionId(String sessionId);

    public boolean reconnectPlayer(String roomCode, String clientSessionId, String newSessionId);
}
