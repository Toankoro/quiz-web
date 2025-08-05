package com.example.quizgame.service;

import com.example.quizgame.dto.TemporaryAnswer;
import com.example.quizgame.dto.response.RoomParticipantResponse;
import com.example.quizgame.dto.supportcard.SupportCardResult;
import com.example.quizgame.entity.RoomParticipant;
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
    // delete all temporary question
    public void deleteAllTemporaryAnswers(String roomCode) {
        String pattern = "tempAnswer:" + roomCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    // get list players in room
    private String getRoomParticipantListKey(String pinCode) {
        return String.format(PLAYER_LIST_PREFIX, pinCode);
    }
    private String getTempAnswersKey(String roomCode, String username) {
        return "room:" + roomCode + ":answers:" + username;
    }
    // replay
    // save temporaryAnswer
    public void saveTemporaryAnswer(String roomCode, String username, TemporaryAnswer answer) {
        String key = getTempAnswersKey(roomCode, username);
        redisTemplate.opsForList().rightPush(key, answer);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    // get temporary answer
    public List<TemporaryAnswer> getTemporaryAnswers(String pinCode, String  username) {
        String key = getTempAnswersKey(pinCode, username);
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        return list.stream()
                .map(obj -> objectMapper.convertValue(obj, TemporaryAnswer.class))
                .collect(Collectors.toList());
    }

    public void deleteTemporaryAnswers(String pinCode, String username) {
        redisTemplate.delete(getTempAnswersKey(pinCode, username));
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
