package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.PlayerAnswer;
import com.example.quizgame.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
    @Query("""
    SELECT COUNT(DISTINCT rp.room.id) 
    FROM PlayerAnswer pa
    JOIN pa.roomParticipant rp
    JOIN pa.question q
    JOIN q.quiz quiz
    WHERE quiz.id = :quizId
      AND rp.user.id = :userId
""")
    long countTimesPlayedQuiz(@Param("quizId") Long quizId, @Param("userId") Long userId);
    List<PlayerAnswer> findByRoomParticipant_User_Id(Long userId);
    List<PlayerAnswer> findByRoomParticipant_User_IdAndRoomParticipant_Room_Id(Long userId, Long roomId);
    void deleteByRoomParticipant_User_IdAndRoomParticipant_Room_Id(Long userId, Long roomId);
}
