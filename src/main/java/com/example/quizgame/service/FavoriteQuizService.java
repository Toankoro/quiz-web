package com.example.quizgame.service;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.quiz.FavoriteQuizResponse;
import com.example.quizgame.dto.quiz.QuizSearchResponse;
import com.example.quizgame.entity.FavoriteQuiz;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.FavoriteQuizRepository;
import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.reponsitory.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteQuizService {

        private final FavoriteQuizRepository favoriteQuizRepository;
        private final UserRepository userRepository;
        private final QuizRepository quizRepository;

        public FavoriteQuizService(FavoriteQuizRepository favoriteQuizRepository,
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

        public ApiResponse<String> addToFavorites(Long quizId) {
                User user = getCurrentUser();
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi"));

                if (favoriteQuizRepository.existsByUserAndQuiz(user, quiz)) {
                        return new ApiResponse<>("Bộ câu hỏi đã nằm trong danh sách yêu thích rồi", "Bộ câu hỏi đã nằm trong danh sách yêu thích rồi", null);
                }
                FavoriteQuiz favoriteQuiz = new FavoriteQuiz();
                favoriteQuiz.setUser(user);
                favoriteQuiz.setQuiz(quiz);

                favoriteQuizRepository.save(favoriteQuiz);
                return new ApiResponse<>("Bộ câu hỏi đã nằm trong danh sách yêu thích rồi", "Bộ câu hỏi đã nằm trong danh sách yêu thích rồi", null);
        }

        public List<FavoriteQuizResponse> getFavoritesByUserId(Long userId) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
                List<FavoriteQuiz> favorites = favoriteQuizRepository.findByUserOrderByCreatedAtDesc(user);
                return favorites.stream()
                        .map(FavoriteQuizResponse::fromFavoriteQuizToFavoriteQuizResponse)
                        .collect(Collectors.toList());
        }


        @Transactional
        public ApiResponse<String> removeFromFavorites(Long quizId) {
                User user = getCurrentUser();
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi"));
                if (!favoriteQuizRepository.existsByUserAndQuiz(user, quiz)) {
                        return new ApiResponse<>("Bộ câu hỏi không nằm trong danh sách yêu thích", "Bộ câu hỏi không nằm trong danh sách yêu thích", null);
                }

                favoriteQuizRepository.deleteByUserAndQuiz(user, quiz);
                return new ApiResponse<>("Bộ câu hỏi được xóa thành công khỏi danh sách yêu thích", "Bộ câu hỏi được xóa thành công khỏi danh sách yêu thích", null);
        }

        // get favorite quiz of user
        public ApiResponse<List<QuizSearchResponse>> getUserFavorites(Pageable pageable, User user) {
               Page<QuizSearchResponse> favoritePage = favoriteQuizRepository.findUserFavorites(user.getId(), pageable);
                ApiResponse.Meta meta = new ApiResponse.Meta(
                        pageable.getPageNumber() + 1,
                        pageable.getPageSize(),
                        favoritePage.getTotalPages(),
                        favoritePage.getTotalElements()
                );
                List<QuizSearchResponse> listQuizSearch = favoritePage.stream().toList();
                return new ApiResponse<>(listQuizSearch, "Lấy danh sách bộ câu hỏi yêu thích thành công", meta);


        }


}