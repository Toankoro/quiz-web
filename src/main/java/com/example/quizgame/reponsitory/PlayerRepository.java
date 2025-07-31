package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.GameRoom;
import com.example.quizgame.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findBySessionId(String sessionId);
    List<Player> findByRoom(GameRoom room);
    Optional<Player> findByClientSessionIdAndRoom(String clientSessionId, GameRoom room);
    Optional<Player> findByRoomIdAndSessionId(Long roomId, String sessionId);
    Optional<Player> findByRoom_RoomCodeAndSessionId(String roomCode, String sessionId);
    List<Player> findByRoom_RoomCode(String roomCode);
    Optional<Player> findByClientSessionId(String clientSessionId);
}
