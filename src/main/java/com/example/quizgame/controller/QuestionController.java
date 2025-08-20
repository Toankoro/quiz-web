package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.question.*;
import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.entity.Room;
import com.example.quizgame.reponsitory.GameRankingRepository;
import com.example.quizgame.reponsitory.RoomRepository;
import com.example.quizgame.service.GameRankingService;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.QuestionService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import com.example.quizgame.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final RoomParticipantRedisService roomParticipantRedisService;
    private final QuestionRedisService questionRedisService;

    @PostMapping("/room/{pinCode}/submit-answer")
    public ResponseEntity<AnswerResult> submitAnswer(
            @PathVariable String pinCode,
            @RequestBody AnswerMessage message,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (message == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            AnswerResult result = questionService.handleAnswer(pinCode, userDetails.getUser(), message);
            
            if (result == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @PostMapping("/{pinCode}/next-question")
    public ResponseEntity<?> nextQuestion(
            @PathVariable String pinCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Room roomDB = roomRepository.findByPinCode(pinCode)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với pinCode này!"));

        if (!roomService.isHostRoom(roomDB.getId(), userDetails.getUser())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Người dùng không phải chủ phòng");
        }

        Map<Object, Object> allSessions = roomParticipantRedisService.getAllSessions(pinCode);
        if (allSessions == null || allSessions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Phòng chưa có người chơi nào!");
        }

        QuestionResponseToParticipant next = questionService.sendNextQuestion(pinCode, null);

        if (next != null) {
            long deadline = System.currentTimeMillis() + next.getLimitedTime() * 1000;
            questionRedisService.setQuestionDeadline(pinCode, next.getId(), deadline);
            questionRedisService.setQuestionStartTime(pinCode, next.getId(), (next.getLimitedTime() + 10) * 1000);
            allSessions.keySet().forEach(sessionId -> {
                messagingTemplate.convertAndSendToUser(
                        sessionId.toString(),
                        "/queue/next-question",
                        next
                );
            });
            return ResponseEntity.ok(next);
        } else {
            questionService.sendGameOver(pinCode);
            return ResponseEntity.noContent().build();
        }
    }

}
