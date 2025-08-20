package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.*;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.service.PlayerAnswerService;
import com.example.quizgame.service.PlayerGameService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/player-answers")
@RequiredArgsConstructor
public class PlayerAnswerController {

    private final PlayerAnswerService playerAnswerService;
    private final RoomParticipantRedisService roomParticipantRedisService;
    private final PlayerGameService service;


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

    // Endpoint để migrate dữ liệu cũ (chỉ dành cho admin)
    @PostMapping("/migrate")
    public ResponseEntity<String> migrateExistingData() {
        playerAnswerService.migrateExistingData();
        return ResponseEntity.ok("Migration hoàn thành");
    }

    @GetMapping("/history")
    public ResponseEntity<List<PlayHistoryDTO>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = userDetails.getId();
        List<PlayHistoryDTO> history = playerAnswerService.getPlayHistory(userId, name, date);
        return ResponseEntity.ok(history);
    }

    // Xóa lịch sử chơi theo room
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        Long userId = userDetails.getId();
        playerAnswerService.deleteHistoryByRoom(userId, roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}")
    public PlayerGameInfoDTO getPlayerGame(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId(); // Lấy userId từ CustomUserDetails
        return service.getPlayerGameInfo(roomId, userId);
    }

}
