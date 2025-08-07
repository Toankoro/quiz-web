package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.answer.TemporaryAnswer;
import com.example.quizgame.dto.question.QuestionResponseToParticipant;
import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.question.ReconnectResponse;
import com.example.quizgame.dto.supportcard.SupportCardResult;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
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

    public QuestionResponse getCurrentQuestion(String roomCode, Long quizId) {
        int currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);
        List<QuestionResponse> questions = questionRedisService.getQuestionsByQuizId(quizId);

        if (currentIndex >= 0 && currentIndex < questions.size()) {
            return questions.get(currentIndex);
        } else {
            throw new IllegalStateException("Không đúng chỉ số của câu hỏi !");
        }
    }

    public AnswerResult handleAnswer(String pinCode, String username, AnswerMessage message) {
        Long questionId = questionRedisService.getCurrentQuestionId(pinCode);
        Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        QuestionResponse question = questionRedisService.getQuestionById(quizId, questionId);

        if (question == null) return null;

        // Tìm người chơi theo username
        RoomParticipant roomParticipant = roomParticipantRepository.findByRoom_PinCodeAndUser_Username(pinCode, username).orElse(null);
        if (roomParticipant == null) return null;
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
            switch (card.getCardType()) {
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
        AnswerResult temp = new AnswerResult(
                message.getClientSessionId(),
                roomParticipant.getId(),
                message.getSelectedAnswer(),
                score,
                isCorrect,
                timeTaken
        );
        roomParticipantRedisService.saveAnswer(pinCode,question.getId(), message.getClientSessionId(), temp);

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

    public QuestionResponseToParticipant sendNextQuestion(String pinCode, String clientSessionId) {
        Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
        List<QuestionResponse> questions = questionRedisService.getQuestionsByQuizId(quizId);

        if (questions == null || questions.isEmpty()) return null;

        int currentIndex = questionRedisService.getCurrentQuestionIndex(pinCode);
        if (currentIndex >= questions.size()) return null;

        if (currentIndex > 0) {
            Long prevId = questions.get(currentIndex - 1).getId();
            Question prev = questionRepository.findById(prevId).orElse(null);
            if (prev != null) {
                handleUnansweredPlayers(pinCode, prev);
            }
        }

//        clearAllSupportCards(pinCode);
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

            if (!submittedAnswers.containsKey(clientSessionId)) {
                AnswerResult unanswered = new AnswerResult(
                        clientSessionId,
                        roomParticipant.getId(),
                        null,
                        0,
                        false,
                        10L
                );

                roomParticipantRedisService.saveUnanswered(pinCode, question.getId(), clientSessionId, unanswered);
            }
        }

        roomParticipantRedisService.setExpire(pinCode, question.getId(), 1, TimeUnit.DAYS);
    }


    public void sendGameOver(String pinCode) {
        messagingTemplate.convertAndSend("/topic/room/" + pinCode + "/game-over", "Đã hết câu hỏi.");
    }

    // using support card for question
    public void useSupportCard(String pinCode, String clientSessionId, SupportCardType cardType) {
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(pinCode);
        if (currentQuestionId != null) {
            SupportCardResult result = new SupportCardResult(currentQuestionId, cardType);
            questionRedisService.saveSupportCard(pinCode, clientSessionId, result);
        }
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

    // reset room state
    public void resetRoomState(String pinCode, Long quizId, String clientSessionId) {

//        questionRedisService.clearAllSupportCards(roomCode);
        questionRedisService.setQuizIdByPinCode(pinCode, quizId);
        questionRedisService.setCurrentQuestionIndex(pinCode, 0);
        questionRedisService.setCurrentQuestionId(pinCode, null);
        roomParticipantRedisService.deleteAnswerHistory(pinCode, clientSessionId);
        questionRedisService.getQuestionsByQuizId(quizId).forEach(question -> roomParticipantRedisService.deleteAnswers(pinCode, question.getId()));
//        roomParticipantRedisService.getPlayersInRoom(roomCode)
//                .forEach(player -> playerRedisService.deleteTemporaryAnswers(roomCode, player.getClientSessionId()));
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
