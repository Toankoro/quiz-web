package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findById(Long id);
}
