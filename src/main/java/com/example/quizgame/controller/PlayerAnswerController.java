package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.service.PlayerAnswerService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/player-answers")
@RequiredArgsConstructor
public class PlayerAnswerController {

    private final PlayerAnswerService playerAnswerService;
    private final RoomParticipantRedisService answerHistoryService;

    @PostMapping("/save/{pinCode}/{clientSessionId}")
    public ResponseEntity<String> saveHistory(
            @PathVariable String pinCode,
            @PathVariable String clientSessionId) {

        List<AnswerResult> history = answerHistoryService.getAnswerHistory(pinCode, clientSessionId);

        if (history.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có lịch sử câu trả lời");
        }

        playerAnswerService.saveAnswersFromHistory(pinCode, clientSessionId, history);

        answerHistoryService.deleteAnswerHistory(pinCode, clientSessionId);

        return ResponseEntity.ok("Lưu lịch sử thành công");
    }
}
