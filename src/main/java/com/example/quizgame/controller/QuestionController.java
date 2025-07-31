package com.example.quizgame.controller;

import com.example.quizgame.dto.*;
import com.example.quizgame.dto.response.*;
import com.example.quizgame.reponsitory.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.quizgame.service.GameRoomService;
import com.example.quizgame.service.QuestionRedisService;
import com.example.quizgame.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin("*")
@RequiredArgsConstructor
@Controller
@RestController
public class QuestionController {
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionService questionService;
    private final SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;
    private final QuestionRedisService questionRedisService;
    private final GameRoomService gameRoomService;
    private final PlayerRepository playerRepository;
    //chơi lại game vừa kết thúc, trưởng nhóm phụ trách nhấn phần chơi lại để người chơi có thể chơi lại được
    @MessageMapping("/room/{roomCode}/replay")
    public void replayGame(@DestinationVariable String roomCode, @Payload ReplayGameMessage replayGameMessage) {
        if (!gameRoomService.isHost(roomCode, replayGameMessage.getHostClientSessionId())) {
            log.warn("Người dùng không phải host cố gắng replay game trong phòng {}", roomCode);
            return;
        }
        questionService.resetRoomState(roomCode, replayGameMessage.getQuizId());
        QuestionResponse firstQuestion = questionService.startGameAndGetFirstQuestion(roomCode,
                replayGameMessage.getQuizId());
        if (firstQuestion != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/start", "Game replayed");
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/question", firstQuestion);
            QuestionTimerResponse timer = questionService.createQuestionTimer(roomCode, firstQuestion.getId());
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/timer", timer);
            sendLeaderboardUpdate(roomCode);
        } else {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/game-over", "Không có câu hỏi.");
        }
    }

    @MessageMapping("/room/{roomCode}/support-card")
    public void useSupportCard(@DestinationVariable String roomCode,
            @Payload SupportCardMessage supportCardMessage, @Header("simpSessionId") String simpSessionId) {
        // Kiểm tra xem người chơi đã sử dụng card cho câu hỏi hiện tại chưa
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(roomCode);
        if (currentQuestionId == null) {
            log.warn("Không có câu hỏi hiện tại trong phòng {}", roomCode);
            return;
        }

        SupportCardResult existingCard = questionRedisService.getSupportCard(roomCode,
                supportCardMessage.getSessionId());
        if (existingCard != null && existingCard.getQuestionId().equals(currentQuestionId)) {
            log.warn("Người chơi {} đã sử dụng card cho câu hỏi hiện tại trong phòng {}",
                    supportCardMessage.getSessionId(), roomCode);
            return;
        }

        // Lưu thông tin card
        questionService.useSupportCard(roomCode, supportCardMessage.getSessionId(), supportCardMessage.getCardType());
        log.info("Áp dụng thẻ {} cho người chơi {} trong phòng {}",
                supportCardMessage.getCardType(), supportCardMessage.getSessionId(), roomCode);

        if (supportCardMessage.getCardType() == SupportCardType.HIDE_ANSWER) {
            String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
            QuestionResponse currentQuestion = questionRedisService.getQuestionById(quizId, currentQuestionId);

            if (currentQuestion != null) {
                QuestionResponse hidden = questionService.hideTwoAnswers(currentQuestion);
                messagingTemplate.convertAndSendToUser(
                        simpSessionId,
                        "/queue/question-updated",
                        hidden,
                        createHeaders(simpSessionId));

                log.info("Đã áp dụng card ẩn câu trả lời cho người chơi {} trong phòng {}",
                        supportCardMessage.getSessionId(), roomCode);
            }
        }
    }

    @MessageMapping("/room/{roomCode}/start")
    public void startGame(@DestinationVariable String roomCode, @Payload StartGameMessage startGameMessage) {
        if (!gameRoomService.isHost(roomCode, startGameMessage.getHostClientSessionId())) {
            log.warn("Người dùng không phải host cố gắng start game trong phòng {}", roomCode);
            return;
        }

        QuestionResponse firstQuestion = questionService.startGameAndGetFirstQuestion(roomCode,
                startGameMessage.getQuizId());
        if (firstQuestion != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/start", "Game started");
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/question", firstQuestion);

            // Gửi thông tin timer cho câu hỏi đầu tiên
            QuestionTimerResponse timer = questionService.createQuestionTimer(roomCode, firstQuestion.getId());
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/timer", timer);

            // Gửi bảng điểm ban đầu khi bắt đầu game
            sendLeaderboardUpdate(roomCode);
        } else {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/game-over", "Không có câu hỏi.");
        }
    }

    @MessageMapping("/room/{roomCode}/next-question")
    public void nextQuestion(@DestinationVariable String roomCode, @Payload NextQuestionMessage nextQuestionMessage) {
        // Kiểm tra quyền host
        if (!gameRoomService.isHost(roomCode, nextQuestionMessage.getHostClientSessionId())) {
            log.warn("Người dùng không phải host cố gắng next question trong phòng {}", roomCode);
            return;
        }

        QuestionResponse next = questionService.sendNextQuestion(roomCode);
        if (next != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/question", next);
            QuestionTimerResponse timer = questionService.createQuestionTimer(roomCode, next.getId());
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/timer", timer);

            // Gửi bảng điểm cập nhật sau khi chuyển câu hỏi
            sendLeaderboardUpdate(roomCode);
        } else {
            // Gửi bảng điểm cuối cùng trước khi kết thúc game
            sendLeaderboardUpdate(roomCode);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/game-over", "Đã hết câu hỏi.");
        }
    }

    // submit answer and send result
    @MessageMapping("/room/{roomCode}/submit-answer")
    public void receiveAnswer(@DestinationVariable String roomCode,
            AnswerMessage message,
            @Header("simpSessionId") String simpSessionId) {
        AnswerResult result = questionService.handleAnswer(roomCode, message);
        log.info("Gửi kết quả đến người chơi");
        messagingTemplate.convertAndSendToUser(
                simpSessionId,
                "/queue/answer-result",
                result,
                createHeaders(simpSessionId));

        // Gửi bảng điểm cập nhật cho tất cả người chơi
        sendLeaderboardUpdate(roomCode);
    }

    // Handle reconnect
    @MessageMapping("/room/{roomCode}/reconnect")
    public void reconnect(@DestinationVariable String roomCode,
            @Payload ReconnectMessage reconnectMessage,
            @Header("simpSessionId") String newSessionId) {

        log.info("Người chơi {} reconnect vào phòng {}", reconnectMessage.getPlayerName(), roomCode);

        ReconnectResponse response = questionService.handleReconnect(
                roomCode,
                reconnectMessage.getClientSessionId(),
                newSessionId);

        if (response.isSuccess()) {
            messagingTemplate.convertAndSendToUser(
                    newSessionId,
                    "/queue/reconnect",
                    response,
                    createHeaders(newSessionId));
            if (!response.isHasAnsweredCurrentQuestion()) {
                messagingTemplate.convertAndSendToUser(
                        newSessionId,
                        "/queue/question",
                        response.getCurrentQuestion(),
                        createHeaders(newSessionId));
            }
            if (response.getLastAnswerResult() != null) {
                messagingTemplate.convertAndSendToUser(
                        newSessionId,
                        "/queue/answer-result",
                        response.getLastAnswerResult(),
                        createHeaders(newSessionId));
            }
            // gửi bảng điểm hiện tại
            LeaderboardResponse leaderboard = questionService.getLeaderboard(roomCode);
            messagingTemplate.convertAndSendToUser(
                    newSessionId,
                    "/queue/leaderboard",
                    leaderboard,
                    createHeaders(newSessionId));
        } else {
            // Gửi thông báo lỗi
            messagingTemplate.convertAndSendToUser(
                    newSessionId,
                    "/queue/reconnect-error",
                    response,
                    createHeaders(newSessionId));
        }
    }

    // REST endpoint để lấy bảng điểm cửa người chơi
    @GetMapping("/api/room/{roomCode}/leaderboard")
    public LeaderboardResponse getLeaderboard(@PathVariable String roomCode) {
        return questionService.getLeaderboard(roomCode);
    }

    // REST endpoint để kiểm tra xem người chơi có thể sử dụng support card hay không
    @GetMapping("/api/room/{roomCode}/player/{sessionId}/can-use-card")
    public boolean canUseSupportCard(@PathVariable String roomCode, @PathVariable String sessionId) {
        return questionService.canUseSupportCard(roomCode, sessionId);
    }

    // Gửi bảng điểm cập nhật cho tất cả người chơi trong phòng
    private void sendLeaderboardUpdate(String roomCode) {
        LeaderboardResponse leaderboard = questionService.getLeaderboard(roomCode);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/leaderboard", leaderboard);
        log.info("Đã gửi bảng điểm cập nhật cho phòng {}", roomCode);
    }

    // CreateHeader
    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

}
