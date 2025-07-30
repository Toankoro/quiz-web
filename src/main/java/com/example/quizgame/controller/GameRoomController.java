package com.example.quizgame.controller;

import com.example.quizgame.dto.request.CreateRoomRequest;
import com.example.quizgame.dto.response.CreateRoomResponse;
import com.example.quizgame.entity.GameRoom;
import com.example.quizgame.service.GameRoomService;
import com.example.quizgame.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final PlayerService playerService;
// tạo phòng
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request) {
        GameRoom room = gameRoomService.createRoom(request.getHostUsername(), request.getQuizId(),
                request.getHostClientSessionId());
        CreateRoomResponse response = new CreateRoomResponse(
                room.getHostUsername(),
                room.getRoomCode(),
                room.getHostClientSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<GameRoom> getRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(gameRoomService.getRoomByCode(roomCode));
    }

    @GetMapping("/{roomCode}/players")
    public ResponseEntity<?> getPlayersInRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(playerService.getPlayersInRoom(roomCode));
    }
}
