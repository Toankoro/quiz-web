package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.answer.HistoryDetailDTO;
import com.example.quizgame.dto.answer.HistorySummaryDTO;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.service.PlayerAnswerService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/player-answers")
@RequiredArgsConstructor
public class PlayerAnswerController {

    private final PlayerAnswerService playerAnswerService;
    private final RoomParticipantRedisService roomParticipantRedisService;

    @PostMapping("/save/{pinCode}/{clientSessionId}")
    public ResponseEntity<String> saveHistory(
            @PathVariable String pinCode,
            @PathVariable String clientSessionId) {

        List<AnswerResult> history = roomParticipantRedisService.getAnswerHistory(pinCode, clientSessionId);

        if (history.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có lịch sử câu trả lời");
        }
        // save db and delete redis answer of room participant
        playerAnswerService.saveAnswersFromHistory(pinCode, clientSessionId, history);
        roomParticipantRedisService.deleteAnswerHistory(pinCode, clientSessionId);

        return ResponseEntity.ok("Lưu lịch sử thành công");
    }

    // summary history
    @GetMapping("/summary")
    public ResponseEntity<List<HistorySummaryDTO>> getHistorySummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(playerAnswerService.getHistorySummary(userId));
    }

    // detail history play of user
    @GetMapping("/detail/{roomId}")
    public ResponseEntity<List<HistoryDetailDTO>> getHistoryDetail(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(playerAnswerService.getHistoryDetail(userId, roomId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        playerAnswerService.deleteUserHistory(userId, roomId);
        return ResponseEntity.ok("Xóa lịch sử thành công");
    }

    // Endpoint để migrate dữ liệu cũ (chỉ dành cho admin)
    @PostMapping("/migrate")
    public ResponseEntity<String> migrateExistingData() {
        playerAnswerService.migrateExistingData();
        return ResponseEntity.ok("Migration hoàn thành");
    }
}
