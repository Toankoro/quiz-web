package com.example.quizgame.service;

import com.example.quizgame.dto.SupportCardResult;
import com.example.quizgame.dto.response.QuestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private String getCurrentQuestionIdKey(String roomCode) {
        return "room:" + roomCode + ":question_current";
    }

    private String getCurrentIndexKey(String roomCode) {
        return "room:" + roomCode + ":currentIndex";
    }

    private String getQuizIdKey(String roomCode) {
        return "room:" + roomCode + ":quizId";
    }

    public void setCurrentQuestionId(String roomCode, Long questionId) {
        String key = getCurrentQuestionIdKey(roomCode);
        if (questionId != null) {
            stringRedisTemplate.opsForValue().set(key, questionId.toString());
        } else {
            stringRedisTemplate.delete(key);
        }
    }


    public Long getCurrentQuestionId(String roomCode) {
        String key = getCurrentQuestionIdKey(roomCode);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null)
            return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setCurrentQuestionIndex(String roomCode, int index) {
        redisTemplate.opsForValue().set(getCurrentIndexKey(roomCode), index);
    }

    public int getCurrentQuestionIndex(String roomCode) {
        Object value = redisTemplate.opsForValue().get(getCurrentIndexKey(roomCode));
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof String)
            return Integer.parseInt((String) value);
        return 0;
    }

    public void setQuizIdForRoomCode(String roomCode, String quizId) {
        redisTemplate.opsForValue().set(getQuizIdKey(roomCode), quizId);
    }

    // get quizid by room code cache
    public String getQuizIdByRoomCode(String roomCode) {
        Object value = redisTemplate.opsForValue().get(getQuizIdKey(roomCode));
        return value != null ? value.toString() : null;
    }

    public void saveSupportCard(String roomCode, String sessionId, SupportCardResult result) {
        String key = "card:" + roomCode + ":" + sessionId;
        redisTemplate.opsForValue().set(key, result);
    }

    public SupportCardResult getSupportCard(String roomCode, String sessionId) {
        String key = "card:" + roomCode + ":" + sessionId;
        Object value = redisTemplate.opsForValue().get(key);
        return objectMapper.convertValue(value, SupportCardResult.class);
    }

    public void removeSupportCard(String roomCode, String sessionId) {
        String key = "card:" + roomCode + ":" + sessionId;
        redisTemplate.delete(key);
    }

    /**
     * Xóa tất cả support cards trong phòng
     */
    public void clearAllSupportCards(String roomCode) {
        String pattern = "card:" + roomCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // clear() cache câu hỏi nếu update, insert, create
    public void clearCachedQuestionsByQuizId(String quizId) {
        String redisKey = "quiz:" + quizId + ":questions";
        redisTemplate.delete(redisKey);
    }

    public QuestionResponse getQuestionById(String quizId, Long questionId) {
        String redisKey = "quiz:" + quizId + ":questions";
        Object cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null && cached instanceof List<?>) {
            return ((List<?>) cached).stream()
                    .filter(obj -> obj instanceof Map)
                    .map(obj -> QuestionResponse.convertFromMap((Map<String, Object>) obj))
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // Quản lý thời gian cho câu hỏi
    private String getQuestionTimerKey(String roomCode, Long questionId) {
        return "timer:" + roomCode + ":" + questionId;
    }

    public void setQuestionStartTime(String roomCode, Long questionId) {
        String key = getQuestionTimerKey(roomCode, questionId);
        redisTemplate.opsForValue().set(key + ":start", System.currentTimeMillis());
    }

    public Long getQuestionStartTime(String roomCode, Long questionId) {
        String key = getQuestionTimerKey(roomCode, questionId);
        Object value = redisTemplate.opsForValue().get(key + ":start");
        return value != null ? (Long) value : null;
    }

    public void clearQuestionTimer(String roomCode, Long questionId) {
        String key = getQuestionTimerKey(roomCode, questionId);
        redisTemplate.delete(key + ":start");
    }
}
