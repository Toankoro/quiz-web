package com.example.quizgame.service;

import com.example.quizgame.dto.supportcard.SupportCardResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.entity.Question;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepo;

    // key get current question id
    private String getCurrentQuestionIdKey(String pinCode) {
        return "room:" + pinCode + ":question_current";
    }
    // key get current index question
    private String getCurrentIndexKey(String pinCode) {
        return "room:" + pinCode + ":currentIndex";
    }
    // key quiz id
    private String getQuizIdKey(String pinCode) {
        return "room:" + pinCode + ":quizId";
    }

    // set current question id
    public void setCurrentQuestionId(String pinCode, Long questionId) {
        String key = getCurrentQuestionIdKey(pinCode);
        if (questionId != null) {
            stringRedisTemplate.opsForValue().set(key, questionId.toString());
        } else {
            stringRedisTemplate.delete(key);
        }
    }
    // get current question id
    public Long getCurrentQuestionId(String pinCode) {
        String key = getCurrentQuestionIdKey(pinCode);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null)
            return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    // set current question index
    public void setCurrentQuestionIndex(String pinCode, int index) {
        redisTemplate.opsForValue().set(getCurrentIndexKey(pinCode), index);
    }
    // get current question index
    public int getCurrentQuestionIndex(String pinCode) {
        Object value = redisTemplate.opsForValue().get(getCurrentIndexKey(pinCode));
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof String)
            return Integer.parseInt((String) value);
        return 0;
    }
    // set quizId for pinCode
    public void setQuizIdByPinCode(String pinCode, Long quizId) {
        redisTemplate.opsForValue().set(getQuizIdKey(pinCode), quizId);
    }
    // get quizid by pinCode
    public Long getQuizIdByPinCode(String pinCode) {
        Object value = redisTemplate.opsForValue().get(getQuizIdKey(pinCode));
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    // save support card for user
    public void saveSupportCard(String roomCode, String username, SupportCardResult result) {
        String key = "card:" + roomCode + ":" + username;
        redisTemplate.opsForValue().set(key, result);
    }

    // get support card for user
    public SupportCardResult getSupportCard(String roomCode, String username) {
        String key = "card:" + roomCode + ":" + username;
        Object value = redisTemplate.opsForValue().get(key);
        return objectMapper.convertValue(value, SupportCardResult.class);
    }

    public void removeSupportCard(String pinCode, String username) {
        String key = "card:" + pinCode + ":" + username;
        redisTemplate.delete(key);
    }
    public QuestionResponse getQuestionById(Long quizId, Long questionId) {
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
    public List<QuestionResponse> getQuestionsByQuizId(Long quizId) {
        String redisKey = "quiz:" + quizId + ":questions";

        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null && cached instanceof List<?>) {
            List<?> rawList = (List<?>) cached;
            List<QuestionResponse> result = new ArrayList<>();

            for (Object obj : rawList) {
                QuestionResponse qr = objectMapper.convertValue(obj, QuestionResponse.class);
                result.add(qr);
            }

            return result;
        }

        List<Question> questionEntities = questionRepo.findByQuizId(quizId);
        List<QuestionResponse> questions = questionEntities.stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(redisKey, questions, 30, TimeUnit.MINUTES);
        return questions;
    }

    // startTime for question
    public void setQuestionStartTime(String roomCode, Long questionId, long startTime) {
        String key = "questionStartTime:" + roomCode;
        redisTemplate.opsForHash().put(key, questionId.toString(), String.valueOf(startTime));
    }

    public long getQuestionStartTime(String roomCode, Long questionId) {
        String key = "questionStartTime:" + roomCode;
        Object value = redisTemplate.opsForHash().get(key, questionId.toString());
        if (value == null) throw new RuntimeException("Start time not found for question");
        return Long.parseLong(value.toString());
    }



}
