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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomParticipantRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // delete all temporary question
    public void deleteAllTemporaryAnswers(String roomCode) {
        String pattern = "tempAnswer:" + roomCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    // get list players in room
    private String getPlayerListKey(String pinCode) {
        return "room:" + pinCode + ":players";
    }

    // add participant to room
    public void addRoomParticipantToRoom(String pinCode, RoomParticipant roomParticipant) {
        String key = getPlayerListKey(pinCode);
        RoomParticipantResponse response = RoomParticipantResponse.fromRoomParticipantToResponse(roomParticipant);
        redisTemplate.opsForList().rightPush(key, response);
    }
    // get list roomparticipant in room
    public List<RoomParticipantResponse> getRoomParticipantInRoom(String roomCode) {
        String key = getPlayerListKey(roomCode);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0)
            return new ArrayList<>();
        return redisTemplate.opsForList().range(key, 0, size - 1)
                .stream()
                .map(obj -> objectMapper.convertValue(obj, RoomParticipantResponse.class))
                .toList();
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

}
