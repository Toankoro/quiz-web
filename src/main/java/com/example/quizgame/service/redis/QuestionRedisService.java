package com.example.quizgame.service.redis;

import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.entity.Question;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepo;

    private static final int MAX_RANDOM_TIMES = 3;

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

    public void clearAllCardsInRoom(String pinCode) {
        Set<String> keys = redisTemplate.keys("card:*:" + pinCode + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete("card:locked:" + pinCode);
    }

    public void useCardForQuestion(String pinCode, String clientSessionId, Long questionId, SupportCardType cardType) {
        String availableKey = "card:available:" + pinCode + ":" + clientSessionId;
        String usedKey = "card:used:" + pinCode + ":" + clientSessionId;

        Boolean hasCard = redisTemplate.opsForSet().isMember(availableKey, cardType.toString());
        if (!hasCard) {
            throw new IllegalStateException("Thẻ không khả dụng hoặc đã dùng.");
        }
        Boolean alreadyUsed = redisTemplate.opsForHash().hasKey(usedKey, questionId.toString());

        if (alreadyUsed) {
            throw new IllegalStateException("Đã dùng thẻ cho câu hỏi này.");
        }
        redisTemplate.opsForHash().put(usedKey, questionId.toString(), cardType.toString());
        redisTemplate.opsForSet().remove(availableKey, cardType.toString());
    }

    public SupportCardType getUsedCardForQuestion(String pinCode, String clientSessionId, Long questionId) {
        String usedKey = "card:used:" + pinCode + ":" + clientSessionId;
        Object cardObj = redisTemplate.opsForHash().get(usedKey, questionId.toString());
        if (cardObj != null) {
            try {
                return SupportCardType.valueOf(cardObj.toString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public boolean isCardUsedForQuestion(String pinCode, String clientSessionId, Long questionId) {
        String usedKey = "card:used:" + pinCode + ":" + clientSessionId;
        Boolean alreadyUsed = redisTemplate.opsForHash().hasKey(usedKey, questionId.toString());
        return Boolean.TRUE.equals(alreadyUsed);
    }

    public Set<String> getAvailableCards(String pinCode, String clientSessionId) {
        String key = "card:available:" + pinCode + ":" + clientSessionId;
        Set<Object> rawSet = redisTemplate.opsForSet().members(key);
        return rawSet.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public void lockRoomAndCommitCards(String pinCode) {
        String lockKey = "card:locked:" + pinCode;
        redisTemplate.opsForValue().set(lockKey, "locked");

        // Lấy danh sách người chơi trong phòng
        Set<Object> clientSessionIds = redisTemplate.opsForHash().keys("room:" + pinCode + ":sessions");

        for (Object sessionIdObj : clientSessionIds) {
            String clientSessionId = sessionIdObj.toString();
            String randomedKey = "card:randomed:" + pinCode + ":" + clientSessionId;
            String availableKey = "card:available:" + pinCode + ":" + clientSessionId;

            Set<Object> randomedCards = redisTemplate.opsForSet().members(randomedKey);
            if (randomedCards != null) {
                for (Object card : randomedCards) {
                    redisTemplate.opsForSet().add(availableKey, card.toString());
                }
            }
        }
    }

    public List<SupportCardType> randomTwoCards(String pin, String clientSessionId) {
        String roomLockKey = "card:locked:" + pin;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(roomLockKey))) {
            throw new IllegalStateException("Không thể random sau khi phòng đã bắt đầu");
        }

        String countKey = "card:random_count:" + pin + ":" + clientSessionId;
        String randomedKey = "card:randomed:" + pin + ":" + clientSessionId;

        Long currentCount = redisTemplate.opsForValue().increment(countKey);
        if (currentCount != null && currentCount > MAX_RANDOM_TIMES) {
            throw new IllegalStateException("Bạn đã random quá số lần cho phép");
        }

        redisTemplate.delete(randomedKey); // xóa thẻ cũ

        List<SupportCardType> allCards = new ArrayList<>(EnumSet.allOf(SupportCardType.class));
        Collections.shuffle(allCards);
        List<SupportCardType> selected = allCards.subList(0, 2);

        for (SupportCardType card : selected) {
            redisTemplate.opsForSet().add(randomedKey, card.name());
        }

        return selected;
    }
    // get question by id from redis in memory
    public List<QuestionResponse> getQuestionsByQuizId(Long quizId) {
        String redisKey = "quiz:" + quizId + ":questions:list";

        List<Object> cached = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .map(obj -> objectMapper.convertValue(obj, QuestionResponse.class))
                    .collect(Collectors.toList());
        }

        List<Question> questionEntities = questionRepo.findByQuizId(quizId);
        List<QuestionResponse> questions = questionEntities.stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .collect(Collectors.toList());

        // Lưu vào Redis dưới dạng List
        for (QuestionResponse q : questions) {
            redisTemplate.opsForList().rightPush(redisKey, q);
        }
        redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);

        return questions;
    }

    public void clearQuestionsCache(Long quizId) {
        String redisKey = "quiz:" + quizId + ":questions:list";
        redisTemplate.delete(redisKey);
    }



    public QuestionResponse getQuestionById(Long quizId, Long questionId) {
        String redisKey = "quiz:" + quizId + ":questions:list";

        List<Object> cachedList = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (cachedList != null && !cachedList.isEmpty()) {
            for (Object obj : cachedList) {
                QuestionResponse qr = objectMapper.convertValue(obj, QuestionResponse.class);
                if (qr.getId().equals(questionId)) {
                    return qr;
                }
            }
        }

        Question question = questionRepo.findByIdAndQuiz_Id(questionId, quizId)
                .orElse(null);

        if (question == null) {
            return null;
        }

        QuestionResponse response = QuestionResponse.fromQuestionToQuestionResponse(question);

        if (cachedList == null || cachedList.isEmpty()) {
            List<Question> allQuestions = questionRepo.findByQuizId(quizId);
            for (Question q : allQuestions) {
                redisTemplate.opsForList().rightPush(redisKey, QuestionResponse.fromQuestionToQuestionResponse(q));
            }
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        } else {
            redisTemplate.opsForList().rightPush(redisKey, response);
        }

        return response;
    }


    // set, get startTime for question
    public void setQuestionStartTime(String roomCode, Long questionId, long durationMillis) {
        String key = "question:start:" + roomCode + ":" + questionId;
        long startTime = System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, startTime, durationMillis, TimeUnit.MILLISECONDS);
    }

    // Lấy
    public long getQuestionStartTime(String roomCode, Long questionId) {
        String key = "question:start:" + roomCode + ":" + questionId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) throw new RuntimeException("Start time không có cho câu hỏi này");
        return Long.parseLong(value.toString());
    }


    public void setQuestionDeadline(String pinCode, Long questionId, long durationMillis) {
        String key = "question:deadline:" + pinCode + ":" + questionId;
        long deadline = System.currentTimeMillis() + durationMillis;
        redisTemplate.opsForValue().set(key, deadline, durationMillis + 5000, TimeUnit.MILLISECONDS);
    }

    public Long getQuestionDeadline(String pinCode, Long questionId) {
        String key = "question:deadline:" + pinCode + ":" + questionId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    public void deleteQuestionDeadline(String pinCode) {
        String key = "question:deadline:" + pinCode + ":*";
        redisTemplate.delete(key);
    }


}
