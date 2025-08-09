package com.example.quizgame.service.redis;

import com.example.quizgame.dto.answer.AnswerResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomParticipantRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final long EXPIRATION_DAYS = 1;

    // save answer room participant with question id
    private String getRedisKey(String pinCode, Long questionId) {
        return "answer:" + pinCode + ":" + questionId.toString();
    }

    public void saveUnanswered(String pinCode, Long questionId, String clientSessionId, AnswerResult answerResult) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.opsForHash().putIfAbsent(redisKey, clientSessionId, answerResult);
    }

    public void saveAnswer(String pinCode, Long questionId, String clientSessionId, AnswerResult answerResult) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.opsForHash().put(redisKey, clientSessionId, answerResult);
    }

    public Map<String, AnswerResult> getSubmittedAnswers(String pinCode, Long questionId) {
        String redisKey = getRedisKey(pinCode, questionId);
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(redisKey);

        Map<String, AnswerResult> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            Object value = entry.getValue();
            AnswerResult answerResult;

            if (value instanceof LinkedHashMap) {
                // Deserialize manually
                answerResult = objectMapper.convertValue(value, AnswerResult.class);
            } else if (value instanceof AnswerResult) {
                answerResult = (AnswerResult) value;
            } else {
                throw new IllegalStateException("Unsupported value type in Redis: " + value.getClass());
            }

            result.put(entry.getKey().toString(), answerResult);
        }

        return result;
    }

    public AnswerResult getAnswer(String pinCode, Long questionId, String clientSessionId) {
        String redisKey = getRedisKey(pinCode, questionId);
        Object result = redisTemplate.opsForHash().get(redisKey, clientSessionId);
        return result != null ? (AnswerResult) result : null;
    }

    public void setExpire(String pinCode, Long questionId, long duration, TimeUnit unit) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.expire(redisKey, duration, unit);
    }

    public void deleteAnswers(String pinCode, Long questionId) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.delete(redisKey);
    }

    public void deleteAnswerRoomParticipant(String pinCode, Long questionId, String clientSessionId) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.opsForHash().delete(redisKey, clientSessionId);
    }


    // history answer pinCode of participant
    public void deleteAnswerHistory(String pinCode, String clientSessionId) {
        String key = getHistoryRoomParticipantKey(pinCode, clientSessionId);
        redisTemplate.delete(key);
    }

    public void saveAnswerHistory(String pinCode, String clientSessionId, AnswerResult answerResult) {
        String key = getHistoryRoomParticipantKey(pinCode, clientSessionId);
        redisTemplate.opsForList().rightPush(key, answerResult);
    }

    public List<AnswerResult> getAnswerHistory(String pinCode, String clientSessionId) {
        String key = getHistoryRoomParticipantKey(pinCode, clientSessionId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        return rawList.stream()
                .map(obj -> (AnswerResult) obj)
                .collect(Collectors.toList());
    }

    public void setHistoryExpire(String pinCode, String clientSessionId, long timeout, TimeUnit unit) {
        String key = getHistoryRoomParticipantKey(pinCode, clientSessionId);
        redisTemplate.expire(key, timeout, unit);
    }

    private String getHistoryRoomParticipantKey(String pinCode, String clientSessionId) {
        return "history:" + pinCode + ":" + clientSessionId;
    }

    // get, remove, get username, get all client session id  - list players in room
    private String getSessionHashKey(String pinCode) {
        return "room:" + pinCode + ":sessions";
    }

    public String createAndStoreClientSession(String pinCode, String username) {
        String clientSessionId = UUID.randomUUID().toString();
        String hashKey = getSessionHashKey(pinCode);

        redisTemplate.opsForHash().put(hashKey, clientSessionId, username);
        redisTemplate.expire(hashKey, EXPIRATION_DAYS, TimeUnit.DAYS);

        return clientSessionId;
    }

    public Map<Object, Object> getAllSessions(String roomCode) {
        String hashKey = getSessionHashKey(roomCode);
        return redisTemplate.opsForHash().entries(hashKey);
    }

    public Set<String> getAllUsernames(String pinCode) {
        String hashKey = getSessionHashKey(pinCode);
        return redisTemplate.opsForHash().values(hashKey)
                .stream().map(Object::toString)
                .collect(Collectors.toSet());
    }

    public void removeClientSession(String pinCode, String clientSessionId) {
        String hashKey = getSessionHashKey(pinCode);
        redisTemplate.opsForHash().delete(hashKey, clientSessionId);
    }

    public void removeAllSessions(String pinCode) {
        String hashKey = getSessionHashKey(pinCode);
        redisTemplate.delete(hashKey);
    }


}
