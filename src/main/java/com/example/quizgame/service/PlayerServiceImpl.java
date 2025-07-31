package com.example.quizgame.service;

import com.example.quizgame.dto.AnswerMessage;
import com.example.quizgame.dto.PlayerAnswer;
import com.example.quizgame.dto.response.PlayerResponse;
import com.example.quizgame.entity.GameRoom;
import com.example.quizgame.entity.Player;
import com.example.quizgame.entity.Question;
import com.example.quizgame.reponsitory.GameRoomRepository;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.PlayerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final GameRoomRepository gameRoomRepository;
    private final QuestionRepository questionRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final PlayerRedisService redisService;

    @Override
    public List<PlayerResponse> getPlayersInRoom(String roomCode) {
        return redisService.getPlayersInRoom(roomCode);
    }

    public Player registerPlayer(String playerName, String roomCode, String wsSessionId, String clientSessionId) {
        // Kiểm tra trùng tên trong Redis
        boolean nameExists = getPlayersInRoom(roomCode).stream()
                .anyMatch(p -> p.getName() != null && p.getName().equalsIgnoreCase(playerName));
        if (nameExists) {
            throw new RuntimeException("Tên người chơi đã tồn tại trong phòng.");
        }

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        Optional<Player> existing = playerRepository.findByClientSessionIdAndRoom(clientSessionId, room);
        Player player;
        if (existing.isPresent()) {
            player = existing.get();
            player.setSessionId(wsSessionId);
        } else {
            player = new Player();
            player.setName(playerName);
            player.setSessionId(wsSessionId);
            player.setClientSessionId(clientSessionId);
            player.setRoom(room);
        }
        player = playerRepository.save(player);
        redisService.addPlayerToRoom(roomCode, player);
        return player;
    }
    // ??????
    @Override
    public void saveAnswer(AnswerMessage message) {
        Player player = playerRepository.findBySessionId(message.getSessionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người chơi"));

        Question question = questionRepository.findById(message.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        PlayerAnswer answer = new PlayerAnswer();
        answer.setPlayer(player);
        answer.setQuestion(question);
        answer.setSelectedAnswer(message.getSelectedAnswer());
        answer.setCorrect(message.getSelectedAnswer().equals(question.getCorrectAnswer()));
        answer.setAnsweredAt(LocalDateTime.now());

        playerAnswerRepository.save(answer);
    }

    @Override
    public List<PlayerAnswer> getPlayerAnswers(Long playerId) {
        return playerAnswerRepository.findByPlayerId(playerId);
    }

    @Override
    public boolean reconnectPlayer(String roomCode, String clientSessionId, String newSessionId) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        Optional<Player> existing = playerRepository.findByClientSessionIdAndRoom(clientSessionId, room);
        if (existing.isPresent()) {
            Player player = existing.get();
            player.setSessionId(newSessionId);
            playerRepository.save(player);
            boolean updated = redisService.updateSessionIdByClientSessionId(roomCode, clientSessionId, newSessionId);
            return updated;
        }
        return false;
    }

    public Optional<String> removePlayerBySessionId(String sessionId) {
        Optional<PlayerResponse> playerOpt = redisService.removePlayerBySessionId(sessionId);
        if (playerOpt.isPresent()) {
            PlayerResponse playerResponse = playerOpt.get();
            // Remove from DB
            if (playerResponse.getClientSessionId() != null && playerResponse.getRoomCode() != null) {
                Optional<Player> playerDbOpt = playerRepository.findByClientSessionIdAndRoom(
                        playerResponse.getClientSessionId(),
                        gameRoomRepository.findByRoomCode(playerResponse.getRoomCode()).orElse(null));
                playerDbOpt.ifPresent(playerRepository::delete);
            }
            return Optional.ofNullable(playerResponse.getRoomCode());
        }
        return Optional.empty();
    }
}
