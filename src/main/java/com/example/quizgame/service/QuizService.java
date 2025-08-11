package com.example.quizgame.service;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.quiz.QuizRequest;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.quiz.QuizResponse;
import com.example.quizgame.dto.quiz.QuizSearchResponse;
import com.example.quizgame.dto.user.UserProfileResponse;
import com.example.quizgame.entity.PlayerAnswer;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final FavoriteQuizRepository favoriteQuizRepository;
    private final QuestionRepository questionRepository;
    private final PlayerAnswerRepository playerAnswerRepository;

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

    private QuizResponse convertToQuizResponse(Quiz quiz, User currentUser) {
        boolean isFavorite = favoriteQuizRepository.existsByUserAndQuiz(currentUser, quiz);

        UserProfileResponse creatorProfile = UserProfileResponse.builder()
                .firstname(quiz.getCreatedBy().getFirstname())
                .email(quiz.getCreatedBy().getEmail())
                .socialLinks(quiz.getCreatedBy().getSocialLinks())
                .avatar(quiz.getCreatedBy().getAvatar())
                .build();

        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(QuestionResponse::fromQuestionToQuestionResponse)
                .toList();

        long countTimePlayQuiz = playerAnswerRepository.countTimesPlayedQuiz(quiz.getId(), currentUser.getId());



        return QuizResponse.builder()
                .id(quiz.getId())
                .topic(quiz.getTopic())
                .name(quiz.getName())
                .description(quiz.getDescription())
                .visibleTo(quiz.isVisibleTo())
                .questions(questions)
                .favorite(isFavorite)
                .createdBy(creatorProfile)
                .createdAt(quiz.getCreatedAt())
                .countTimePlay(countTimePlayQuiz)
                .build();
    }

    public ApiResponse<QuizResponse> createQuiz(
            QuizRequest quizRequest,
            MultipartFile quizImage,
            List<MultipartFile> questionImages,
            User user) {

        Quiz quiz = buildQuizFromRequest(quizRequest, user);

        if (quizImage != null && !quizImage.isEmpty()) {
            quiz.setImageUrl(convertImageToBase64(quizImage));
        }

        if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty() && questionImages != null) {
            for (int i = 0; i < quiz.getQuestions().size(); i++) {
                Question question = quiz.getQuestions().get(i);
                if (i < questionImages.size() && questionImages.get(i) != null && !questionImages.get(i).isEmpty()) {
                    question.setImageUrl(convertImageToBase64(questionImages.get(i)));
                }
                question.setQuiz(quiz);
            }
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return new ApiResponse<>(convertToQuizResponse(savedQuiz, user), "Người dùng tạo bộ câu hỏi thành công!");
    }

    public static String convertImageToBase64(MultipartFile image) {
        try {
            if (image.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Kích thước ảnh vượt quá 2MB");
            }
            String contentType = image.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                return "data:" + contentType + ";base64," +
                        Base64.getEncoder().encodeToString(image.getBytes());
            } else {
                throw new IllegalArgumentException("File upload không phải ảnh");
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xử lý ảnh: " + e.getMessage(), e);
        }
    }

    public ApiResponse<List<QuizResponse>> getAllQuizzesOfUser(User user, Pageable pageable) {
        Page<Quiz> quizPage = quizRepository.findByCreatedBy(user.getId(), pageable);
        List<QuizResponse> quizResponses = quizPage
                .map(quiz -> convertToQuizResponse(quiz, user))
                .stream().toList();

        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements()
        );

        return new ApiResponse<>(quizResponses, "Lấy danh sách bộ câu hỏi theo người dùng thành công !", meta);
    }


    public ApiResponse<QuizResponse> getQuizById(Long quizId, User user) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        return new ApiResponse<>(convertToQuizResponse(quiz, user), "Người dùng lấy bộ câu hỏi thành công !");

    }

    public ApiResponse<List<QuizResponse>> getAllQuizzes(Pageable pageable, User user) {
        Page<Quiz> quizPage = quizRepository.findAll(pageable);
        List<QuizResponse> responses = quizPage.getContent().stream().map(quiz -> convertToQuizResponse(quiz, user)).toList();
        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements()
        );
        return new ApiResponse<>(responses, "Lấy danh sách bộ câu hỏi thành công", meta);
    }

    public ApiResponse<List<QuizSearchResponse>> searchQuizzes(String topic, String name, Pageable pageable) {
        Page<Quiz> quizzes = quizRepository.searchByTopicAndName(topic, name, pageable);

        List<QuizSearchResponse> response = quizzes.stream()
                .map(QuizSearchResponse::fromQuizToQuizSearchResponse)
                .collect(Collectors.toList());

        ApiResponse.Meta meta = new ApiResponse.Meta(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                quizzes.getTotalPages(),
                quizzes.getTotalElements()
        );

        return new ApiResponse<>(response, "Tìm kiếm thành công", meta);
    }


    @Transactional
    public Quiz cloneQuiz(Long quizId, User newOwner) {
        Quiz originalQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quiz gốc"));

        Quiz clonedQuiz = new Quiz();
        clonedQuiz.setTopic(originalQuiz.getTopic());
        clonedQuiz.setName(originalQuiz.getName());
        clonedQuiz.setDescription(originalQuiz.getDescription());
        clonedQuiz.setVisibleTo(originalQuiz.isVisibleTo());
        clonedQuiz.setImageUrl(originalQuiz.getImageUrl());
        clonedQuiz.setCreatedBy(newOwner);
        clonedQuiz.setCreatedAt(LocalDateTime.now());

        quizRepository.save(clonedQuiz);

        for (Question originalQ : originalQuiz.getQuestions()) {
            Question clonedQ = new Question();
            clonedQ.setContent(originalQ.getContent());
            clonedQ.setDescription(originalQ.getDescription());
            clonedQ.setAnswerA(originalQ.getAnswerA());
            clonedQ.setAnswerB(originalQ.getAnswerB());
            clonedQ.setAnswerC(originalQ.getAnswerC());
            clonedQ.setAnswerD(originalQ.getAnswerD());
            clonedQ.setImageUrl(originalQ.getImageUrl());
            clonedQ.setCorrectAnswer(originalQ.getCorrectAnswer());
            clonedQ.setLimitedTime(originalQ.getLimitedTime());
            clonedQ.setScore(originalQ.getScore());
            clonedQ.setQuiz(clonedQuiz);

            questionRepository.save(clonedQ);
        }

        return clonedQuiz;
    }


    public QuizResponse updateQuiz(Long quizId, QuizRequest request, User currentUser) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        if (!quiz.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa quiz này");
        }

        quiz.setTopic(request.getTopic());
        quiz.setName(request.getName());
        quiz.setDescription(request.getDescription());
        quiz.setVisibleTo(request.isVisibleTo());
        quiz.setImageUrl(request.getImageUrl());

        if (request.getQuestions() != null) {
            quiz.getQuestions().clear();
            request.getQuestions().forEach(q -> {
                Question question = new Question();
                question.setContent(q.getContent());
                question.setQuiz(quiz);
                quiz.getQuestions().add(question);
            });
        }

        Quiz updatedQuiz = quizRepository.save(quiz);
        return this.convertToQuizResponse(updatedQuiz, currentUser);
    }


}
