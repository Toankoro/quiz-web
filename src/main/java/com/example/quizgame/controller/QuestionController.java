package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.question.*;
import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.supportcard.SupportCardMessage;
import com.example.quizgame.dto.supportcard.SupportCardResult;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Room;
import com.example.quizgame.reponsitory.RoomRepository;
import com.example.quizgame.service.QuestionRedisService;
import com.example.quizgame.service.QuestionService;
import com.example.quizgame.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final QuestionRedisService questionRedisService;

    @PostMapping("/room/{pinCode}/submit-answer")
    public ResponseEntity<AnswerResult> submitAnswer(
            @PathVariable String pinCode,
            @RequestParam String clientSessionId,
            @RequestBody AnswerMessage message,
            Principal principal) {

        if (message == null) {
            System.out.println("Câu trả lời null!");
            return ResponseEntity.badRequest().build();
        }
        String username = principal.getName();
        AnswerResult result = questionService.handleAnswer(pinCode, username, message);
        messagingTemplate.convertAndSendToUser(
                clientSessionId,
                "/queue/answer-result",
                result
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{pinCode}/next-question")
    public ResponseEntity<?> nextQuestion(
            @PathVariable String pinCode,
            @RequestBody HostRoomRequest nextQuestionMessage,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        Room roomDB = roomRepository.findByPinCode(pinCode).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với phù hợp với pinCode đang tham gia !"));
        if (!roomService.isHostRoom(roomDB.getId(), userDetails.getUser())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Người dùng không phải chủ phòng");
        }
        QuestionResponseToParticipant next = questionService.sendNextQuestion(pinCode, nextQuestionMessage.getClientSessionId());
        if (next != null) {
            messagingTemplate.convertAndSendToUser(
                    nextQuestionMessage.getClientSessionId(),
                    "/queue/next-question",
                    next
            );
            return ResponseEntity.ok(next);
        } else {
            questionService.sendGameOver(pinCode);
            return ResponseEntity.noContent().build();
        }
    }


    @PostMapping("/{pinCode}/support-card")
    public ResponseEntity<?> useSupportCard(
            @PathVariable String pinCode,
            @RequestBody SupportCardMessage supportCardMessage) {

        Long currentQuestionId = questionRedisService.getCurrentQuestionId(pinCode);
        if (currentQuestionId == null) {
            return ResponseEntity.badRequest().body("Không có câu hỏi hiện tại.");
        }

        SupportCardResult existingCard = questionRedisService.getSupportCard(pinCode, supportCardMessage.getClientSessionId());
        if (existingCard != null && existingCard.getQuestionId().equals(currentQuestionId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Bạn đã sử dụng thẻ cho câu hỏi hiện tại.");
        }

        questionService.useSupportCard(pinCode, supportCardMessage.getClientSessionId(), supportCardMessage.getCardType());

        if (supportCardMessage.getCardType() == SupportCardType.HIDE_ANSWER) {
            Long quizId = questionRedisService.getQuizIdByPinCode(pinCode);
            QuestionResponse currentQuestion = questionRedisService.getQuestionById(quizId, currentQuestionId);

            if (currentQuestion != null) {
                QuestionResponse hidden = questionService.hideTwoAnswers(currentQuestion);
                return ResponseEntity.ok(hidden);
            }
        }

        return ResponseEntity.ok("Đã sử dụng thẻ: " + supportCardMessage.getCardType().name());
    }

    @PostMapping("/{pinCode}/reset-room")
    public ResponseEntity<?> replayGameRest(
            @PathVariable String pinCode,
            @RequestParam Long quizId,
            @RequestParam String clientSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Room room = roomRepository.findByPinCode(pinCode).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng tương ứng với pinCode"));
        if (!roomService.isHostRoom(room.getId(), userDetails.getUser())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền replay game trong phòng này.");
        }
        questionService.resetRoomState(pinCode, quizId, clientSessionId);
        return ResponseEntity.ok("Trạng thái phòng đã được reset.");
    }


    @GetMapping("/reconnect")
    public ResponseEntity<ReconnectResponse> reconnect(@RequestParam String pinCode, @RequestParam String clientSessionId) {
        ReconnectResponse response = questionService.handleReconnect(pinCode, clientSessionId);
        return ResponseEntity.ok(response);
    }

}
