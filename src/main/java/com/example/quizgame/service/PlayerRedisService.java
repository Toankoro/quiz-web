package com.example.quizgame.service;

import com.example.quizgame.dto.TemporaryAnswer;
import com.example.quizgame.dto.response.AnswerResult;
import com.example.quizgame.dto.response.PlayerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.quizgame.entity.Player;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    // xóa temp answer of room
    public void deleteAllTemporaryAnswers(String roomCode) {
        String pattern = "tempAnswer:" + roomCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String getPlayerListKey(String roomCode) {
        return "room:" + roomCode + ":players";
    }

    public void addPlayerToRoom(String roomCode, Player player) {
        String key = getPlayerListKey(roomCode);
        PlayerResponse response = PlayerResponse.fromPlayerToPlayerResponse(player);
        redisTemplate.opsForList().rightPush(key, response);
    }

    public List<PlayerResponse> getPlayersInRoom(String roomCode) {
        String key = getPlayerListKey(roomCode);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0)
            return new ArrayList<>();
        return redisTemplate.opsForList().range(key, 0, size - 1)
                .stream()
                .map(obj -> objectMapper.convertValue(obj, PlayerResponse.class))
                .toList();
    }

    public Optional<PlayerResponse> removePlayerBySessionId(String sessionId) {
        for (String key : redisTemplate.keys("room:*:players")) {
            List<Object> players = redisTemplate.opsForList().range(key, 0, -1);
            if (players != null) {
                for (int i = 0; i < players.size(); i++) {
                    PlayerResponse player = objectMapper.convertValue(players.get(i), PlayerResponse.class);
                    if (player.getSessionId().equals(sessionId)) {
                        redisTemplate.opsForList().remove(key, 1, players.get(i));
                        return Optional.of(player);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateSessionIdByClientSessionId(String roomCode, String clientSessionId, String newSessionId) {
        String key = getPlayerListKey(roomCode);
        List<Object> players = redisTemplate.opsForList().range(key, 0, -1);
        if (players == null)
            return false;

        for (int i = 0; i < players.size(); i++) {
            PlayerResponse player = objectMapper.convertValue(players.get(i), PlayerResponse.class);
            if (clientSessionId.equals(player.getClientSessionId())) {
                player.setSessionId(newSessionId);
                redisTemplate.opsForList().set(key, i, player);
                return true;
            }
        }
        return false;
    }

    private String getTempAnswersKey(String roomCode, String clientSessionId) {
        return "room:" + roomCode + ":answers:" + clientSessionId;
    }
    // xem xét lưu danh sách của nhiều người thay vì lưu từng người
    public void saveTemporaryAnswer(String roomCode, String clientSessionId, TemporaryAnswer answer) {
        String key = getTempAnswersKey(roomCode, clientSessionId);
        redisTemplate.opsForList().rightPush(key, answer);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public List<TemporaryAnswer> getTemporaryAnswers(String roomCode, String clientSessionId) {
        String key = getTempAnswersKey(roomCode, clientSessionId);
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        return list.stream()
                .map(obj -> objectMapper.convertValue(obj, TemporaryAnswer.class))
                .collect(Collectors.toList());
    }

    public void deleteTemporaryAnswers(String roomCode, String clientSessionId) {
        redisTemplate.delete(getTempAnswersKey(roomCode, clientSessionId));
    }

    public boolean hasAnsweredQuestion(String roomCode, String clientSessionId, Long questionId) {
        List<TemporaryAnswer> answers = getTemporaryAnswers(roomCode, clientSessionId);
        return answers.stream()
                .anyMatch(answer -> answer.getQuestionId().equals(questionId));
    }

    public boolean hasAnsweredCurrentQuestion(String roomCode, String clientSessionId, Long currentQuestionId) {
        return hasAnsweredQuestion(roomCode, clientSessionId, currentQuestionId);
    }

    public AnswerResult getLastAnswerResult(String roomCode, String clientSessionId) {
        List<TemporaryAnswer> answers = getTemporaryAnswers(roomCode, clientSessionId);
        if (answers.isEmpty()) {
            return null;
        }

        TemporaryAnswer lastAnswer = answers.get(answers.size() - 1);
        AnswerResult result = new AnswerResult();
        result.setSessionId(clientSessionId);
        result.setCorrect(lastAnswer.isCorrect());
        result.setScore(lastAnswer.getScore());
        return result;
    }

}
