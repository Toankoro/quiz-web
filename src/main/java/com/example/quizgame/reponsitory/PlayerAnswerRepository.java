package com.example.quizgame.reponsitory;

import com.example.quizgame.dto.answer.PlayHistoryDTO;
import com.example.quizgame.entity.PlayerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

  @Query("""
        SELECT new com.example.quizgame.dto.answer.PlayHistoryDTO(
            pa.room.quiz.topic,
            pa.room.quiz.name,
            pa.room.quiz.id,
            pa.room.id,
            COUNT(DISTINCT pa.question),
            MIN(pa.answeredAt)
        )
        FROM PlayerAnswer pa
        WHERE pa.user.id = :userId
          AND (:name IS NULL OR LOWER(pa.room.quiz.name) LIKE LOWER(CONCAT('%', :name, '%')))
        GROUP BY pa.room.id, pa.room.quiz.topic, pa.room.quiz.name, pa.room.quiz.id
        ORDER BY MIN(pa.answeredAt) DESC
    """)
  List<PlayHistoryDTO> findHistoryNoDate(
          @Param("userId") Long userId,
          @Param("name") String name
  );

  // Lấy lịch sử chơi theo ngày
  @Query("""
        SELECT new com.example.quizgame.dto.answer.PlayHistoryDTO(
            pa.room.quiz.topic,
            pa.room.quiz.name,
            pa.room.quiz.id,
            pa.room.id,
            COUNT(DISTINCT pa.question),
            MIN(pa.answeredAt)
        )
        FROM PlayerAnswer pa
        WHERE pa.user.id = :userId
          AND (:name IS NULL OR LOWER(pa.room.quiz.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND FUNCTION('DATE', pa.answeredAt) = :date
        GROUP BY pa.room.id, pa.room.quiz.topic, pa.room.quiz.name, pa.room.quiz.id
        ORDER BY MIN(pa.answeredAt) DESC
    """)
  List<PlayHistoryDTO> findHistoryWithDate(
          @Param("userId") Long userId,
          @Param("name") String name,
          @Param("date") LocalDate date
  );


  // Xóa câu trả lời theo user và room
  @Modifying
  @Query("DELETE FROM PlayerAnswer pa WHERE pa.user.id = :userId AND pa.room.id = :roomId")
  void deleteByUserAndRoom(@Param("userId") Long userId, @Param("roomId") Long roomId);


}
