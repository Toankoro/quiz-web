package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findById(Long id);

    @Query("SELECT q FROM Quiz q WHERE q.createdBy.id = :userId")
    Page<Quiz> findByCreatedBy(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT q FROM Quiz q " +
            "WHERE (:topic IS NULL OR LOWER(q.topic) LIKE LOWER(CONCAT('%', :topic, '%'))) " +
            "AND (:name IS NULL OR LOWER(q.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Quiz> searchByTopicAndName(@Param("topic") String topic,
                                    @Param("name") String name,
                                    Pageable pageable);
}
