package com.example.quizgame.service;

import com.example.quizgame.dto.response.FavoriteQuizResponse;
import com.example.quizgame.dto.response.QuestionResponse;
import com.example.quizgame.entity.FavoriteQuiz;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.FavoriteQuizRepository;
import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.reponsitory.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteQuizServiceImpl implements FavoriteQuizService {

        private final FavoriteQuizRepository favoriteQuizRepository;
        private final UserRepository userRepository;
        private final QuizRepository quizRepository;

        public FavoriteQuizServiceImpl(FavoriteQuizRepository favoriteQuizRepository,
                        UserRepository userRepository,
                        QuizRepository quizRepository) {
                this.favoriteQuizRepository = favoriteQuizRepository;
                this.userRepository = userRepository;
                this.quizRepository = quizRepository;
        }

        private User getCurrentUser() {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        }

        @Override
        public boolean addToFavorites(Long quizId) {
                User user = getCurrentUser();
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi"));

                if (favoriteQuizRepository.existsByUserAndQuiz(user, quiz)) {
                        return false;
                }
                // tên người dùng cần khác nhau
                FavoriteQuiz favoriteQuiz = new FavoriteQuiz();
                favoriteQuiz.setUser(user);
                favoriteQuiz.setQuiz(quiz);

                favoriteQuizRepository.save(favoriteQuiz);
                return true;
        }

        @Override
        public List<FavoriteQuizResponse> getFavoritesByUserId(Long userId) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
                List<FavoriteQuiz> favorites = favoriteQuizRepository.findByUserOrderByCreatedAtDesc(user);
                return favorites.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());
        }
        @Transactional
        @Override
        public boolean removeFromFavorites(Long quizId) {
                User user = getCurrentUser();
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi"));

                if (!favoriteQuizRepository.existsByUserAndQuiz(user, quiz)) {
                        return false;
                }

                favoriteQuizRepository.deleteByUserAndQuiz(user, quiz);
                return true;
        }

        @Override
        public List<FavoriteQuizResponse> getUserFavorites() {
                User user = getCurrentUser();
                List<FavoriteQuiz> favorites = favoriteQuizRepository.findByUserOrderByCreatedAtDesc(user);
                return favorites.stream()
                                .map(this::convertToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public boolean isFavorite(Long quizId) {
                User user = getCurrentUser();
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi"));
                return favoriteQuizRepository.existsByUserAndQuiz(user, quiz);
        }

        private FavoriteQuizResponse convertToResponse(FavoriteQuiz favoriteQuiz) {
                Quiz quiz = favoriteQuiz.getQuiz();

                List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                                .map(q -> new QuestionResponse(
                                                q.getId(),
                                                q.getContent(),
                                                q.getDescription(),
                                                q.getAnswerA(),
                                                q.getAnswerB(),
                                                q.getAnswerC(),
                                                q.getAnswerD(),
                                                q.getImageUrl(),
                                                q.getCorrectAnswer(),
                                                q.getLimitedTime(),
                                                q.getScore()))
                                .collect(Collectors.toList());

                return new FavoriteQuizResponse(
                                favoriteQuiz.getId(),
                                quiz.getId(),
                                quiz.getTopic(),
                                favoriteQuiz.getCreatedAt(),
                                questionResponses);
        }
}