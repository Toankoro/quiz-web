package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.answer.TemporaryAnswer;
import com.example.quizgame.dto.response.AnswerResult;
import com.example.quizgame.dto.response.QuestionResponse;
import com.example.quizgame.dto.supportcard.SupportCardResult;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionService {
    private final SimpMessagingTemplate messagingTemplate;
    private final QuestionRedisService questionRedisService;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomParticipantRedisService roomParticipantRedisService;

    public AnswerResult handleAnswer(String pinCode, String username, AnswerMessage message) {
        Long questionId = questionRedisService.getCurrentQuestionId(pinCode);
        String quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        QuestionResponse question = questionRedisService.getQuestionById(quizId, questionId);

        if (question == null) return null;

        // Tìm người chơi theo username
        RoomParticipant roomParticipant = roomParticipantRepository.findByRoom_PinCodeAndUser_Username(pinCode, username).orElse(null);
        if (roomParticipant == null) return null;

        List<TemporaryAnswer> tempAnswers = roomParticipantRedisService.getTemporaryAnswers(pinCode, username);
        int baseTimeLimit = 10000;
        int baseScore = question.getScore() != null ? question.getScore() : 200;
        long timeTaken = message.getTimeTaken() != null ? message.getTimeTaken() : baseTimeLimit;
        int timeLimit = baseTimeLimit;

        SupportCardResult card = questionRedisService.getSupportCard(pinCode, username);
        String selectedAnswer = message.getSelectedAnswer();
        boolean isCorrect = selectedAnswer != null &&
                question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer);
        int score = isCorrect ? calculateScore(timeTaken, timeLimit, baseScore) : 0;

        if (card != null && card.getQuestionId().equals(questionId)) {
            switch (card.getType()) {
                case DOUBLE_SCORE:
                    if (isCorrect) score *= 2;
                    break;
                case HALF_SCORE:
                    if (isCorrect) score = (int) (score * 0.5);
                    break;
                case SKIP_QUESTION:
                    isCorrect = false;
                    score = 0;
                    break;
                case HIDE_ANSWER:
                    break;
            }
            questionRedisService.removeSupportCard(pinCode, username);
        }

        // Lưu câu trả lời tạm thời
        TemporaryAnswer temp = new TemporaryAnswer(
                question.getId(),
                message.getSelectedAnswer(),
                isCorrect,
                score,
                timeTaken
        );
        roomParticipantRedisService.saveTemporaryAnswer(pinCode, username, temp);

        // Trả về kết quả
        AnswerResult result = new AnswerResult();
        result.setCorrect(isCorrect);
        result.setScore(score);

        return result;
    }


    private int calculateScore(long timeTakenMillis, int timeLimitMillis, int maxScore) {
        if (timeTakenMillis > timeLimitMillis) {
            return 0;
        }
        double ratio = (double) (timeLimitMillis - timeTakenMillis) / timeLimitMillis;
        ratio = Math.max(0.1, ratio);
        return (int) (maxScore * ratio);
    }


}
