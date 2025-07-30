package com.example.quizgame.controller;

import com.example.quizgame.dto.request.QuizRequest;
import com.example.quizgame.dto.response.QuestionResponse;
import com.example.quizgame.dto.response.QuizResponse;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.service.FavoriteQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/questions")
public class QuizController {
    private final QuizRepository quizRepository;
    private final FavoriteQuizService favoriteQuizService;

    public QuizController(QuizRepository quizRepository, FavoriteQuizService favoriteQuizService) {
        this.quizRepository = quizRepository;
        this.favoriteQuizService = favoriteQuizService;
    }

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(@RequestBody QuizRequest quizRequest) {
        Quiz quiz = new Quiz();
        quiz.setTitle(quizRequest.getTitle());
        List<Question> questions = quizRequest.getQuestions().stream().map(qr -> {
            Question q = new Question();
            q.setContent(qr.getContent());
            q.setAnswerA(qr.getAnswerA());
            q.setAnswerB(qr.getAnswerB());
            q.setAnswerC(qr.getAnswerC());
            q.setAnswerD(qr.getAnswerD());
            q.setCorrectAnswer(qr.getCorrectAnswer());
            q.setScore(qr.getScore() != null ? qr.getScore() : Integer.valueOf(200));
            q.setQuiz(quiz);
            return q;
        }).collect(Collectors.toList());
        quiz.setQuestions(questions);
        Quiz quizDB = quizRepository.save(quiz);
        QuizResponse quizResponse = new QuizResponse();
        List<QuestionResponse> questionResponses = quizDB.getQuestions().stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getContent(),
                        q.getAnswerA(),
                        q.getAnswerB(),
                        q.getAnswerC(),
                        q.getAnswerD(),
                        q.getImageUrl(),
                        q.getCorrectAnswer(),
                        q.getLimitedTime(),
                        q.getScore()))
                .collect(Collectors.toList());
        quizResponse.setId(quizDB.getId());
        quizResponse.setTitle(quizDB.getTitle());
        quizResponse.setQuestions(questionResponses);
        return ResponseEntity.ok(quizResponse);
    }

    @GetMapping
    public List<QuizResponse> getAllQuizzes(@RequestParam(required = false) Long userId) {
        List<Quiz> quizzes = quizRepository.findAll();

        return quizzes.stream()
                .map(quiz -> {
                    QuizResponse response = new QuizResponse(quiz.getId(), quiz.getTitle(),
                            quiz.getQuestions().stream()
                                    .map(q -> new QuestionResponse(
                                            q.getId(),
                                            q.getContent(),
                                            q.getAnswerA(),
                                            q.getAnswerB(),
                                            q.getAnswerC(),
                                            q.getAnswerD(),
                                            q.getImageUrl(),
                                            q.getCorrectAnswer(),
                                            q.getLimitedTime(),
                                            q.getScore()))
                                    .collect(Collectors.toList()));

                    // Add favorite status if userId is provided
//                    if (userId != null) {
//                        boolean isFavorite = favoriteQuizService.isFavorite(userId, quiz.getId());
//                        response.setFavorite(isFavorite);
//                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        Quiz quiz = quizRepository.findById(id).orElseThrow();
        List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getContent(),
                        q.getAnswerA(),
                        q.getAnswerB(),
                        q.getAnswerC(),
                        q.getAnswerD(),
                        q.getImageUrl(),
                        q.getCorrectAnswer(),
                        q.getLimitedTime(),
                        q.getScore()

                ))
                .collect(Collectors.toList());

        QuizResponse response = new QuizResponse(quiz.getId(), quiz.getTitle(), questionResponses);

//        if (userId != null) {
//            boolean isFavorite = favoriteQuizService.isFavorite(userId, id);
//            response.setFavorite(isFavorite);
//        }

        return ResponseEntity.ok(response);
    }
}
