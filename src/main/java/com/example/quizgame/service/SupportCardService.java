package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.supportcard.SupportCardMessage;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.exceptions.ConflictException;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupportCardService {
    private final QuestionRedisService questionRedisService;
    private final RoomParticipantRedisService roomParticipantRedisService;
    private final QuestionService questionSevice;

    public Object useSupportCard(String pinCode, SupportCardMessage supportCardMessage) throws BadRequestException {
        String clientSessionId = supportCardMessage.getClientSessionId();
        SupportCardType cardType = supportCardMessage.getCardType();

        Long currentQuestionId = questionRedisService.getCurrentQuestionId(pinCode);
        if (currentQuestionId == null) {
            throw new BadRequestException("Không có câu hỏi hiện tại.");
        }

        boolean alreadyUsed = questionRedisService.isCardUsedForQuestion(pinCode, clientSessionId, currentQuestionId);
        if (alreadyUsed) {
            throw new ConflictException("Bạn đã sử dụng thẻ cho câu hỏi hiện tại.");
        }

        Set<String> availableCards = questionRedisService.getAvailableCards(pinCode, clientSessionId);
        if (!availableCards.contains(cardType.name())) {
            throw new BadRequestException("Thẻ không khả dụng hoặc đã sử dụng.");
        }

        if (cardType == SupportCardType.RETRY_ANSWER) {
            AnswerResult existingAnswer = roomParticipantRedisService.getAnswer(pinCode, currentQuestionId, clientSessionId);

            if (existingAnswer == null) {
                throw new BadRequestException("Bạn chưa trả lời câu hỏi.");
            }
            if (existingAnswer.isCorrect()) {
                throw new BadRequestException("Bạn đã trả lời đúng. Không thể sử dụng thẻ này.");
            }
            roomParticipantRedisService.deleteAnswerRoomParticipant(pinCode, currentQuestionId, clientSessionId);
        }

        questionRedisService.useCardForQuestion(pinCode, clientSessionId, currentQuestionId, cardType);

        if (cardType == SupportCardType.HIDE_ANSWER) {
            Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
            QuestionResponse currentQuestion = questionRedisService.getQuestionById(quizId, currentQuestionId);
            if (currentQuestion != null) {
               return questionSevice.hideTwoAnswers(currentQuestion);
            }
        }

        return "Đã sử dụng thẻ: " + cardType.name();
    }


    // reset room state
    public void resetRoomState(String pinCode, Long quizId, String clientSessionId) {
        questionRedisService.clearAllCardsInRoom(pinCode);
        questionRedisService.setQuizIdByPinCode(pinCode, quizId);
        questionRedisService.setCurrentQuestionIndex(pinCode, 0);
        questionRedisService.setCurrentQuestionId(pinCode, null);
        roomParticipantRedisService.deleteAllHistoryOfRoom(pinCode);
        questionRedisService.clearQuestionsCache(quizId);
        roomParticipantRedisService.keepOnlyHost(pinCode);
        questionRedisService.getQuestionsByQuizId(quizId).forEach(question -> roomParticipantRedisService.deleteAnswers(pinCode, question.getId()));
    }
}
