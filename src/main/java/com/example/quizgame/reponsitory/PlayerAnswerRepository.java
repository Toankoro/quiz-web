package com.example.quizgame.reponsitory;

import com.example.quizgame.dto.PlayerAnswer;
import com.example.quizgame.entity.Player;
import com.example.quizgame.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
    List<PlayerAnswer> findByPlayerId(Long playerId);
    Optional<PlayerAnswer> findByPlayerAndQuestion(Player player, Question question);
    Optional<PlayerAnswer> findByPlayerAndQuestionAndSessionId(Player player, Question question, String sessionId);
    boolean existsByPlayerAndQuestionAndSessionId(Player player, Question question, String sessionId);
}
