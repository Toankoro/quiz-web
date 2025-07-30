package com.example.quizgame.service;

import com.example.quizgame.dto.RoomState;
import com.example.quizgame.entity.GameRoom;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.reponsitory.GameRoomRepository;
import com.example.quizgame.reponsitory.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final QuizRepository quizRepository;

    // Tạo phòng chơi game
    public GameRoom createRoom(String hostUsername, Long quizId, String hostClientSessionId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Bộ câu hỏi không tồn tại !"));

        GameRoom gameRoom = new GameRoom();
        gameRoom.setHostUsername(hostUsername);
        gameRoom.setQuiz(quiz);
        gameRoom.setRoomCode(generateRoomCode());
        gameRoom.setRoomState(RoomState.WAITING);
        gameRoom.setCreatedAt(LocalDateTime.now());
        gameRoom.setHostClientSessionId(hostClientSessionId);
        return gameRoomRepository.save(gameRoom);
    }

    public GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
    }

    public void startGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        room.setRoomState(RoomState.PLAYING);
        gameRoomRepository.save(room);
    }

    public void finishGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        room.setRoomState(RoomState.FINISHED);
        gameRoomRepository.save(room);
    }

    public boolean isHost(String roomCode, String clientSessionId) {
        GameRoom room = getRoomByCode(roomCode);
        return room.getHostClientSessionId() != null && room.getHostClientSessionId().equals(clientSessionId);
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }
}
