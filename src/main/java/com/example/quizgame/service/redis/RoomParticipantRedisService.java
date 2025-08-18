package com.example.quizgame.service.redis;

import com.example.quizgame.dto.answer.AnswerResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
        
        if (result == null) {
            return null;
        }
        
        if (result instanceof LinkedHashMap) {
            // Deserialize manually from LinkedHashMap
            return objectMapper.convertValue(result, AnswerResult.class);
        } else if (result instanceof AnswerResult) {
            return (AnswerResult) result;
        } else {
            throw new IllegalStateException("Unsupported value type in Redis: " + result.getClass());
        }
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

    public void deleteAllHistoryOfRoom(String pinCode) {
        String pattern = "history:" + pinCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Đã xóa {} lịch sử trong phòng {}", keys.size(), pinCode);
        } else {
            log.info("Không có lịch sử nào để xóa trong phòng {}", pinCode);
        }
    }

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
                .map(obj -> {
                    if (obj instanceof LinkedHashMap) {
                        // Deserialize manually from LinkedHashMap
                        return objectMapper.convertValue(obj, AnswerResult.class);
                    } else if (obj instanceof AnswerResult) {
                        return (AnswerResult) obj;
                    } else {
                        throw new IllegalStateException("Unsupported value type in Redis history: " + obj.getClass());
                    }
                })
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

        Map<Object, Object> existingSessions = redisTemplate.opsForHash().entries(hashKey);
        existingSessions.forEach((sessionId, user) -> {
            if (username.equals(user.toString())) {
                redisTemplate.opsForHash().delete(hashKey, sessionId);
            }
        });

        // Lưu session mới
        redisTemplate.opsForHash().put(hashKey, clientSessionId, username);
        redisTemplate.expire(hashKey, EXPIRATION_DAYS, TimeUnit.DAYS);

        return clientSessionId;
    }


    public void keepOnlyHost(String pinCode) {
        String hashKey = getSessionHashKey(pinCode);
        Map<Object, Object> sessions = redisTemplate.opsForHash().entries(hashKey);

        if (sessions.isEmpty()) {
            return;
        }

        String hostSessionId = sessions.keySet().iterator().next().toString();

        for (Object sessionId : sessions.keySet()) {
            if (!sessionId.toString().equals(hostSessionId)) {
                redisTemplate.opsForHash().delete(hashKey, sessionId);
            }
        }

        log.info("Giữ lại host sessionId={}, đã xóa {} session khác",
                hostSessionId, sessions.size() - 1);
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

    public boolean isValidClientSession(String pinCode, String clientSessionId) {
        String hashKey = getSessionHashKey(pinCode);
        Map<Object, Object> all = redisTemplate.opsForHash().entries(hashKey);
        log.info("Redis data for room {} = {}", pinCode, all);
        log.info("Check sessionId={} exist? {}", clientSessionId, all.containsKey(clientSessionId));
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(hashKey, clientSessionId));
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
