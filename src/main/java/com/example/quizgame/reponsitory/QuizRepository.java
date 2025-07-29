package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}