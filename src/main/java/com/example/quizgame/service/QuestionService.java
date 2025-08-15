package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.question.QuestionResponseToParticipant;
import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.question.ReconnectResponse;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.entity.*;
import com.example.quizgame.reponsitory.GameRankingRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
import com.example.quizgame.reponsitory.RoomRepository;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionService {
    private final SimpMessagingTemplate messagingTemplate;
    private final QuestionRedisService questionRedisService;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomParticipantRedisService roomParticipantRedisService;
    private final QuestionRepository questionRepository;
    private final RoomRepository roomRepository;
    private final GameRankingService gameRankingService;

    public AnswerResult handleAnswer(String pinCode, User user, AnswerMessage message) {
        Long questionId = questionRedisService.getCurrentQuestionId(pinCode);
        Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        QuestionResponse question = questionRedisService.getQuestionById(quizId, questionId);
        
        if (question == null) {
            return null;
        }

        Room room;
        try {
            room = roomRepository.findByPinCode(pinCode).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng tương ứng với pinCode"));
        } catch (NoSuchElementException e) {
            return null;
        }
        
        RoomParticipant roomParticipant = roomParticipantRepository
                .findByRoom_PinCodeAndUser_Username(pinCode, user.getUsername())
                .orElse(null);

        if (roomParticipant == null) {
            return null;
        }

        int baseTimeLimit = 10000;
        int baseScore = question.getScore() != null ? question.getScore() : 200;
        float timeTaken = message.getTimeTaken() != null ? message.getTimeTaken() : baseTimeLimit;

        String selectedAnswer = message.getSelectedAnswer();
        boolean isCorrect = selectedAnswer != null &&
                question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer);

        int score = isCorrect ? calculateScore(timeTaken, baseTimeLimit, baseScore) : 0;

        SupportCardType usedCard = questionRedisService.getUsedCardForQuestion(
                pinCode,
                message.getClientSessionId(),
                questionId
        );

        if (usedCard != null) {
            switch (usedCard) {
                case DOUBLE_SCORE:
                    if (isCorrect) score *= 2;
                    break;
                case HIDE_ANSWER:
                    break;
                case RETRY_ANSWER:
                    break;
            }
        }

        // save answer temporary
        AnswerResult temp = new AnswerResult(
                questionId,
                message.getClientSessionId(),
                roomParticipant.getId(),
                message.getSelectedAnswer(),
                score,
                isCorrect,
                timeTaken,
                question.getCorrectAnswer()
        );

        roomParticipantRedisService.saveAnswer(pinCode, questionId, message.getClientSessionId(), temp);

        // send result to participant
        AnswerResult result = new AnswerResult(
            questionId,
            message.getClientSessionId(),
            roomParticipant.getId(),
            message.getSelectedAnswer(),
            score,
            isCorrect,
            timeTaken,
            question.getCorrectAnswer()
        );
        
        // THÊM: Set correctAnswer để frontend biết đáp án đúng
        result.setCorrectAnswer(question.getCorrectAnswer());

        roomParticipantRedisService.saveAnswerHistory(pinCode, result.getClientSessionId(), result);
        messagingTemplate.convertAndSendToUser(
                message.getClientSessionId(),
                "/queue/answer-result",
                result
        );
        gameRankingService.addScoreAndCorrect(room, user, result.getScore());
        return result;

    }


    private int calculateScore(float timeTakenMillis, int timeLimitMillis, int maxScore) {
        if (timeTakenMillis > timeLimitMillis) {
            return 0;
        }
        double ratio = (double) (timeLimitMillis - timeTakenMillis) / timeLimitMillis;
        ratio = Math.max(0.1, ratio);
        return (int) (maxScore * ratio);
    }

    public QuestionResponseToParticipant sendNextQuestion(String pinCode, String clientSessionId) {
        Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        List<QuestionResponse> questions = questionRedisService.getQuestionsByQuizId(quizId);

        if (questions == null || questions.isEmpty()) return null;

        int currentIndex = questionRedisService.getCurrentQuestionIndex(pinCode);
        if (currentIndex >= questions.size()) return null;

        // Sửa: Chỉ lấy câu hỏi tiếp theo, không lấy câu hỏi đầu tiên
        if (currentIndex == 0) {
            // Nếu đang ở câu hỏi đầu tiên, tăng index lên 1 để lấy câu hỏi thứ 2
            currentIndex = 1;
            questionRedisService.setCurrentQuestionIndex(pinCode, currentIndex);
        }

        if (currentIndex > 0) {
            Long prevId = questions.get(currentIndex - 1).getId();
            Question prev = questionRepository.findById(prevId).orElse(null);
            if (prev != null) {
                handleUnansweredPlayers(pinCode, prev);
            }
        }

        QuestionResponse next = questions.get(currentIndex);
        if (next == null) return null;

        boolean isLastQuestion = currentIndex + 1 == questions.size();

        questionRedisService.setCurrentQuestionId(pinCode, next.getId());
        questionRedisService.setCurrentQuestionIndex(pinCode, currentIndex + 1);
        return QuestionResponseToParticipant.fromQuestionResponseToQuestionResponseToParticipant(next, isLastQuestion);
    }


    public void handleUnansweredPlayers(String pinCode, Question question) {
        List<RoomParticipant> roomParticipants = roomParticipantRepository
                .findByRoom_PinCode(pinCode)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người chơi trong phòng!"));

        Map<String, AnswerResult> submittedAnswers =
                roomParticipantRedisService.getSubmittedAnswers(pinCode, question.getId());

        for (RoomParticipant roomParticipant : roomParticipants) {
            String clientSessionId = roomParticipant.getClientSessionId(); // Giả định bạn có trường này

            // Thêm guard bỏ qua host (null session) để tránh lỗi "non null hash key required"
            if (clientSessionId == null || clientSessionId.trim().isEmpty()) {
                continue; // Bỏ qua host không có clientSessionId
            }

            if (!submittedAnswers.containsKey(clientSessionId)) {
                                 AnswerResult unanswered = new AnswerResult(
                         question.getId(),
                         clientSessionId,
                         roomParticipant.getId(),
                         null,
                         0,
                         false,
                         Float.parseFloat(question.getLimitedTime().toString()),
                         question.getCorrectAnswer()
                 );

                roomParticipantRedisService.saveUnanswered(pinCode, question.getId(), clientSessionId, unanswered);
                roomParticipantRedisService.saveAnswerHistory(pinCode, clientSessionId, unanswered);
            }
        }

        roomParticipantRedisService.setExpire(pinCode, question.getId(), 1, TimeUnit.DAYS);
    }


    public void sendGameOver(String pinCode) {
        messagingTemplate.convertAndSend("/topic/room/" + pinCode + "/game-over", "Đã hết câu hỏi.");
    }

    public QuestionResponse hideTwoAnswers(QuestionResponse question) {
        if (question == null || question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            return question;
        }
        QuestionResponse clone = QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .answerA(question.getAnswerA())
                .answerB(question.getAnswerB())
                .answerC(question.getAnswerC())
                .answerD(question.getAnswerD())
                .imageUrl(question.getImageUrl())
                .correctAnswer(question.getCorrectAnswer())
                .limitedTime(question.getLimitedTime())
                .score(question.getScore())
                .build();

        String correct = question.getCorrectAnswer().trim().toUpperCase();
        List<String> options = List.of("A", "B", "C", "D");

        List<String> wrongAnswers = options.stream()
                .filter(opt -> !opt.equals(correct))
                .collect(Collectors.toList());

        Collections.shuffle(wrongAnswers);
        List<String> toHide = wrongAnswers.subList(0, Math.min(2, wrongAnswers.size()));

        for (String ans : toHide) {
            switch (ans) {
                case "A" -> clone.setAnswerA(null);
                case "B" -> clone.setAnswerB(null);
                case "C" -> clone.setAnswerC(null);
                case "D" -> clone.setAnswerD(null);
            }
        }

        return clone;
    }

    public ReconnectResponse handleReconnect(String pinCode, String clientSessionId) {
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(pinCode);
        if (currentQuestionId == null) {
            return ReconnectResponse.noCurrentQuestion(clientSessionId);
        }
        Question question = questionRepository.findById(currentQuestionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));
//        boolean hasAnswered = roomParticipantRedisService.hasAnswered(pinCode, clientSessionId, currentQuestionId);
        long startTime = questionRedisService.getQuestionStartTime(pinCode, currentQuestionId);
        return new ReconnectResponse(
                clientSessionId,
                currentQuestionId,
                question.getContent(),
                List.of(question.getAnswerA(), question.getAnswerB(), question.getAnswerC(), question.getAnswerD()),
                startTime,
                question.getLimitedTime(),
                false
        );
    }

}
