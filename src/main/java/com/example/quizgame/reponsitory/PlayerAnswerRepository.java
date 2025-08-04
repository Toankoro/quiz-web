package com.example.quizgame.reponsitory;

import com.example.quizgame.dto.PlayerAnswer;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
//    List<PlayerAnswer> findByPlayerId(Long playerId);
//    Optional<PlayerAnswer> findByPlayerAndQuestion(RoomParticipant roomParticipant, Question question);
//    Optional<PlayerAnswer> findByPlayerAndQuestionAndSessionId(RoomParticipant roomParticipant, Question question, String sessionId);
//    boolean existsByPlayerAndQuestionAndSessionId(RoomParticipant roomParticipant, Question question, String sessionId);
}
