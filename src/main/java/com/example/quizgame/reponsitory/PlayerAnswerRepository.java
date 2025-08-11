package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.PlayerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
//    List<PlayerAnswer> findByPlayerId(Long playerId);
//    Optional<PlayerAnswer> findByPlayerAndQuestion(RoomParticipant roomParticipant, Question question);
//    Optional<PlayerAnswer> findByPlayerAndQuestionAndSessionId(RoomParticipant roomParticipant, Question question, String sessionId);
//    boolean existsByPlayerAndQuestionAndSessionId(RoomParticipant roomParticipant, Question question, String sessionId);
}
