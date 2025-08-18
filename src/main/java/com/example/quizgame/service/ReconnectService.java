package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.question.ReconnectResponse;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReconnectService {

    private final QuestionRedisService questionRedisService;
    private final RoomParticipantRedisService roomParticipantRedisService;

    public ReconnectResponse reconnect(String pinCode, String clientSessionId) {
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(pinCode);
        if (currentQuestionId == null) {
            throw new IllegalStateException("Phòng chưa bắt đầu hoặc không có câu hỏi nào");
        }

        Long deadline = questionRedisService.getQuestionDeadline(pinCode, currentQuestionId);
        long remainingTime = 0L;
        if (deadline != null) {
            remainingTime = Math.max(deadline - System.currentTimeMillis(), 0L);
        }

        AnswerResult existingAnswer = roomParticipantRedisService.getAnswer(pinCode, currentQuestionId, clientSessionId);
        boolean alreadyAnswered = existingAnswer != null;

        Set<String> availableCards = questionRedisService.getAvailableCards(pinCode, clientSessionId);

        Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        QuestionResponse currentQuestion = questionRedisService.getQuestionById(quizId, currentQuestionId);

        return new ReconnectResponse(
                currentQuestionId,
                currentQuestion,
                remainingTime,
                alreadyAnswered,
                availableCards
        );
    }
}
