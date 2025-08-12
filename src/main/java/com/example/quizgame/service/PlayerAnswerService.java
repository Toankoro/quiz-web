package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.entity.PlayerAnswer;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerAnswerService {

    private final PlayerAnswerRepository playerAnswerRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final QuestionRedisService questionRedisService;

    public void saveAnswersFromHistory(String pinCode, String clientSessionId, List<AnswerResult> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        RoomParticipant participant = roomParticipantRepository
                .findByRoom_PinCodeAndClientSessionId(pinCode, clientSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người chơi trong phòng"));

        List<Long> questionIds = history.stream()
                .map(AnswerResult::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Long quizId = participant.getRoom().getQuiz().getId();
        String redisKey = "quiz:" + quizId + ":questions";

        Map<Long, Question> questionMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        for (Long qId : questionIds) {
            Object cached = redisTemplate.opsForHash().get(redisKey, qId.toString());
            if (cached != null) {
                QuestionResponse qr = objectMapper.convertValue(cached, QuestionResponse.class);
                questionMap.put(qId, convertToQuestionEntity(qr)); // Hàm convert bên dưới
            } else {
                missingIds.add(qId);
            }
        }

        if (!missingIds.isEmpty()) {
            questionRepository.findAllById(missingIds)
                    .forEach(q -> questionMap.put(q.getId(), q));
        }

        List<PlayerAnswer> answersToSave = history.stream()
                .map(result -> {
                    Question question = questionMap.get(result.getQuestionId());
                    if (question == null) return null;
                    return PlayerAnswer.builder()
                            .roomParticipant(participant)
                            .clientSessionId(clientSessionId)
                            .question(question)
                            .selectedAnswer(result.getSelectedAnswer())
                            .correct(result.isCorrect())
                            .score(result.getScore())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!answersToSave.isEmpty()) {
            playerAnswerRepository.saveAll(answersToSave);
        }
    }

    private Question convertToQuestionEntity(QuestionResponse qr) {
        if (qr == null) return null;
        return Question.builder()
                .id(qr.getId())
                .content(qr.getContent())
                .answerA(qr.getAnswerA())
                .answerB(qr.getAnswerB())
                .answerC(qr.getAnswerC())
                .answerD(qr.getAnswerD())
                .correctAnswer(qr.getCorrectAnswer())
                .score(qr.getScore())
                .limitedTime(qr.getLimitedTime())
                .build();
    }




}
