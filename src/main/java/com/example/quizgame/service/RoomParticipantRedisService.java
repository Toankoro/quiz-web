package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.answer.TemporaryAnswer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomParticipantRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    private static final String SESSION_PREFIX = "room:%s:clientSession:%s";
    private static final long EXPIRATION_DAYS = 1;
    private static final String PLAYER_LIST_PREFIX = "room:%s:players";

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

    public void setExpire(String pinCode, Long questionId, long duration, TimeUnit unit) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.expire(redisKey, duration, unit);
    }

    public void deleteAnswers(String pinCode, Long questionId) {
        String redisKey = getRedisKey(pinCode, questionId);
        redisTemplate.delete(redisKey);
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

    // get list players in room
    private String getRoomParticipantListKey(String pinCode) {
        return String.format(PLAYER_LIST_PREFIX, pinCode);
    }


    private String getSessionKey(String roomCode, String clientSessionId) {
        return String.format(SESSION_PREFIX, roomCode, clientSessionId);
    }
    public String createAndStoreClientSession(String roomCode, String username) {
        String clientSessionId = UUID.randomUUID().toString();
        String key = getSessionKey(roomCode, clientSessionId);
        redisTemplate.opsForValue().set(key, username, EXPIRATION_DAYS, TimeUnit.DAYS);
        String playerListKey = getRoomParticipantListKey(roomCode);
        redisTemplate.opsForSet().add(playerListKey, username);

        return clientSessionId;
    }

    public String getUsernameFromClientSession(String pinCode, String clientSessionId) {
        String key = getSessionKey(pinCode, clientSessionId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    public void removeClientSession(String roomCode, String clientSessionId) {
        String key = getSessionKey(roomCode, clientSessionId);
        redisTemplate.delete(key);
    }


    public void addRoomParticipant(String pinCode, String username) {
        String roomParticipantListKey = getRoomParticipantListKey(pinCode);
        redisTemplate.opsForSet().add(roomParticipantListKey, username);
    }

    // get room participant list set
    public Set<String> getRoomParticipantList(String pinCode) {
        String playerListKey = getRoomParticipantListKey(pinCode);
        Set<Object> rawSet = redisTemplate.opsForSet().members(playerListKey);

        if (rawSet == null) return Collections.emptySet();

        return rawSet.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public void removeRoomParticipant(String pinCode, String username) {
        String playerListKey = getRoomParticipantListKey(pinCode);
        redisTemplate.opsForSet().remove(playerListKey, username);
    }

    public void clearRoomParticipantList(String pinCode) {
        String playerListKey = getRoomParticipantListKey(pinCode);
        redisTemplate.delete(playerListKey);
    }

}
