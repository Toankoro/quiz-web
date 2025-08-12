package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizId(Long quizId);

    List<Question> findByQuiz_IdOrderByIdAsc(Long quizId);

    List<Question> findAllByQuiz_Id(Long quizId);

    Optional<Question> findByIdAndQuiz_Id(Long questionId, Long quizId);

}
