package com.example.quizgame.service;

import com.example.quizgame.dto.question.QuestionRequest;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QuestionQuizService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;


    public QuestionResponse createQuestion(QuestionRequest request, MultipartFile questionImage) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        Question question = new Question();
        question.setContent(request.getContent());
        question.setDescription(request.getDescription());
        question.setAnswerA(request.getAnswerA());
        question.setAnswerB(request.getAnswerB());
        question.setAnswerC(request.getAnswerC());
        question.setAnswerD(request.getAnswerD());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setLimitedTime(request.getLimitedTime() != null ? request.getLimitedTime() : 10);
        question.setScore(request.getScore() != null ? request.getScore() : 200);
        question.setQuiz(quiz);

        if (questionImage != null && !questionImage.isEmpty()) {
            question.setImageUrl(QuizService.convertImageToBase64(questionImage));
        }

        Question saved = questionRepository.save(question);
        return QuestionResponse.fromQuestionToQuestionResponse(saved);
    }


    public QuestionResponse updateQuestion(Long id, QuestionRequest request, MultipartFile image) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        if (request.getQuizId() != null) {
            Quiz quiz = quizRepository.findById(request.getQuizId())
                    .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));
            question.setQuiz(quiz);
        }

        question.setContent(request.getContent());
        question.setDescription(request.getDescription());
        question.setAnswerA(request.getAnswerA());
        question.setAnswerB(request.getAnswerB());
        question.setAnswerC(request.getAnswerC());
        question.setAnswerD(request.getAnswerD());
        question.setCorrectAnswer(request.getCorrectAnswer());

        question.setLimitedTime(request.getLimitedTime() != null
                ? request.getLimitedTime()
                : question.getLimitedTime());

        question.setScore(request.getScore() != null
                ? request.getScore()
                : question.getScore());

        // Nếu có ảnh mới thì cập nhật
        if (image != null && !image.isEmpty()) {
            question.setImageUrl(QuizService.convertImageToBase64(image));
        } else if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }

        Question updated = questionRepository.save(question);
        return QuestionResponse.fromQuestionToQuestionResponse(updated);
    }


    public ResponseEntity<String> deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy câu hỏi để xóa");
        }
        questionRepository.deleteById(id);
        return ResponseEntity.ok("Xóa câu hỏi thành công");
    }
}
