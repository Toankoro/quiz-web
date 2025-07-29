package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.GameRanking;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRankingRepository extends JpaRepository<GameRanking, Long> {
    Optional<GameRanking> findByRoomAndUser(Room room, User user);
    @Query("SELECT g FROM GameRanking g JOIN FETCH g.user WHERE g.room = :room ORDER BY g.score DESC")
    List<GameRanking> findByRoomOrderByScoreDescWithUser(@Param("room") Room room);
    @Modifying
    @Transactional 
    @Query("DELETE FROM GameRanking g WHERE g.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
