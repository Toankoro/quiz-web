package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.PlayerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
  @Query("""
          SELECT COUNT(DISTINCT pa.room.id)
          FROM PlayerAnswer pa
          JOIN pa.question q
          JOIN q.quiz quiz
          WHERE quiz.id = :quizId
            AND pa.user.id = :userId
      """)
  long countTimesPlayedQuiz(@Param("quizId") Long quizId, @Param("userId") Long userId);

  // Các method sử dụng liên kết trực tiếp với User và Room
  List<PlayerAnswer> findByUser_Id(Long userId);

  List<PlayerAnswer> findByUser_IdAndRoom_Id(Long userId, Long roomId);

  void deleteByUser_IdAndRoom_Id(Long userId, Long roomId);

  // Method để kiểm tra duplicate
  Long countByUser_IdAndRoom_IdAndClientSessionId(Long userId, Long roomId, String clientSessionId);
}
