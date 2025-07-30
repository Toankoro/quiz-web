package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.FavoriteQuiz;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteQuizRepository extends JpaRepository<FavoriteQuiz, Long> {

    List<FavoriteQuiz> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT f.quiz FROM FavoriteQuiz f WHERE f.user = :user")
    List<Quiz> findQuizzesByUser(@Param("user") User user);

    Optional<FavoriteQuiz> findByUserAndQuiz(User user, Quiz quiz);

    boolean existsByUserAndQuiz(User user, Quiz quiz);

    void deleteByUserAndQuiz(User user, Quiz quiz);
}