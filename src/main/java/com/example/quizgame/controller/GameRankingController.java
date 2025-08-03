package com.example.quizgame.controller;

import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.rank.GameRankingDTO;
import com.example.quizgame.entity.Room;
import com.example.quizgame.reponsitory.RoomRepository;
import com.example.quizgame.service.GameRankingService;
import lombok.RequiredArgsConstructor;
import com.example.quizgame.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/gamerank")
@RequiredArgsConstructor
public class GameRankingController {

    private final GameRankingService gameRankingService;
    private final RoomRepository roomRepo;

    @PostMapping("/{roomId}/score")
    public ResponseEntity<?> addScore(@PathVariable Long roomId,
                                      @RequestParam int scoreAdd,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        Room room = roomRepo.findById(roomId).orElseThrow();

        User user = userDetails.getUser();
        gameRankingService.addScoreAndCorrect(room, user, scoreAdd);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/ranking")
    public ResponseEntity<List<GameRankingDTO>> getRanking(
            @PathVariable Long roomId) {

        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        return ResponseEntity.ok(gameRankingService.getRanking(room));
    }
}
