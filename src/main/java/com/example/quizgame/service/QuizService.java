package com.example.quizgame.service;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.request.QuizRequest;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.response.QuizResponse;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.reponsitory.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    public QuizService(QuizRepository quizRepository, UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
    }

    private Quiz buildQuizFromRequest(QuizRequest request, User user) {
        Quiz quiz = new Quiz();
        quiz.setTopic(request.getTopic());
        quiz.setVisibleTo(request.isVisibleTo());
        quiz.setImageUrl(request.getImageUrl());
        quiz.setName(request.getName());
        quiz.setCreatedBy(user);
        quiz.setCreatedAt(LocalDateTime.now());

        List<Question> questions = request.getQuestions().stream().map(qr -> {
            Question q = new Question();
            q.setContent(qr.getContent());
            q.setAnswerA(qr.getAnswerA());
            q.setAnswerB(qr.getAnswerB());
            q.setAnswerC(qr.getAnswerC());
            q.setAnswerD(qr.getAnswerD());
            q.setCorrectAnswer(qr.getCorrectAnswer());
            q.setScore(qr.getScore() != null ? qr.getScore() : 200);
            q.setQuiz(quiz);
            return q;
        }).collect(Collectors.toList());

        quiz.setQuestions(questions);
        return quiz;
    }


    public ApiResponse<QuizResponse> createQuiz(QuizRequest quizRequest, User user) {
        Quiz quiz = buildQuizFromRequest(quizRequest, user);
        Quiz savedQuiz = quizRepository.save(quiz);
        return new ApiResponse<>(convertToQuizResponse(savedQuiz), "Người dùng tạo bộ câu hỏi thành công!");


    }


    //get quiz of user
    private QuizResponse convertToQuizResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .collect(Collectors.toList());

        return new QuizResponse(
                quiz.getId(),
                quiz.getTopic(),
                questions
        );
    }

    public ApiResponse<List<QuizResponse>> getAllQuizzesOfUser(Long userId, Pageable pageable) {

        Page<Quiz> quizPage = quizRepository.findByCreatedBy(userId, pageable);

        List<QuizResponse> quizResponses = quizPage
                .map(this::convertToQuizResponse)
                .getContent();

        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements()
        );

        return new ApiResponse<>(quizResponses, "Lấy danh sách bộ câu hỏi theo người dùng thành công !", meta);
    }


    public ApiResponse<QuizResponse> getQuizById(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .toList();
        return new ApiResponse<>(convertToQuizResponse(quiz), "Người dùng lấy bộ câu hỏi thành công !");

    }

    public ApiResponse<List<QuizResponse>> getAllQuizzes(Pageable pageable) {
        Page<Quiz> quizPage = quizRepository.findAll(pageable);
        List<QuizResponse> responses = quizPage.getContent().stream().map(this::convertToQuizResponse).toList();
        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements()
        );
        return new ApiResponse<>(responses, "Lấy danh sách bộ câu hỏi thành công", meta);
    }

    public ApiResponse<List<QuizResponse>> searchQuizzes(String topic, String name, Pageable pageable) {
        Page<Quiz> quizzes = quizRepository.searchByTopicAndName(topic, name, pageable);
        List<QuizResponse> response = quizzes.stream()
                .map(this::convertToQuizResponse)
                .collect(Collectors.toList());
        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                quizzes.getTotalPages(),
                quizzes.getTotalElements()
        );
        return new ApiResponse<>(response, "Tìm kiếm thành công", meta);
    }


}
