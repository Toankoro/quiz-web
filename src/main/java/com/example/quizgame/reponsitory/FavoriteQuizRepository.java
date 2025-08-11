package com.example.quizgame.reponsitory;

import com.example.quizgame.dto.quiz.QuizSearchResponse;
import com.example.quizgame.entity.FavoriteQuiz;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteQuizRepository extends JpaRepository<FavoriteQuiz, Long> {

    List<FavoriteQuiz> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndQuiz(User user, Quiz quiz);

    void deleteByUserAndQuiz(User user, Quiz quiz);

    @Query("""
    SELECT new com.example.quizgame.dto.quiz.QuizSearchResponse(
        q.id,
        q.topic,
        q.name,
        q.imageUrl,
        q.description,
        size(q.questions),
        q.visibleTo,
        u.username,
        u.avatar,
        q.createdAt
    )
    FROM FavoriteQuiz f
    JOIN f.quiz q
    JOIN q.createdBy u
    WHERE f.user.id = :userId
    ORDER BY f.createdAt DESC
""")
    Page<QuizSearchResponse> findUserFavorites(@Param("userId") Long userId, Pageable pageable);

}