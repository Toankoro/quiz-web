package com.example.quizgame.service;

import com.example.quizgame.dto.*;
import com.example.quizgame.dto.response.*;
import com.example.quizgame.entity.Player;
import com.example.quizgame.entity.Question;
import com.example.quizgame.reponsitory.GameRoomRepository;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.PlayerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final PlayerRepository playerRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final PlayerRedisService playerRedisService;
    private final GameRoomRepository gameRoomRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final QuestionRedisService questionRedisService;
    private final PlayerService playerService;

    // lấy sessionId từ
    public List<String> getSessionIdsInRoom(String roomCode) {
        return playerRepository.findByRoom_RoomCode(roomCode)
                .stream()
                .map(Player::getSessionId)
                .collect(Collectors.toList());
    }

    public boolean hasHideAnswerCard(String roomCode, String sessionId, Long questionId) {
        SupportCardResult card = questionRedisService.getSupportCard(roomCode, sessionId);
        return card != null
                && card.getType() == SupportCardType.HIDE_ANSWER
                && card.getQuestionId().equals(questionId);
    }

    public QuestionResponse hideTwoAnswers(QuestionResponse question) {
        QuestionResponse clone = QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .answerA(question.getAnswerA())
                .answerB(question.getAnswerB())
                .answerC(question.getAnswerC())
                .answerD(question.getAnswerD())
                .imageUrl(question.getImageUrl())
                .correctAnswer(question.getCorrectAnswer())
                .score(question.getScore())
                .build();

        String correct = question.getCorrectAnswer();
        List<String> answers = List.of("A", "B", "C", "D");
        List<String> wrongs = answers.stream()
                .filter(a -> !a.equalsIgnoreCase(correct))
                .collect(Collectors.toList());
        Collections.shuffle(wrongs);
        List<String> toHide = wrongs.subList(0, 2);

        for (String ans : toHide) {
            switch (ans) {
                case "A":
                    clone.setAnswerA(null);
                    break;
                case "B":
                    clone.setAnswerB(null);
                    break;
                case "C":
                    clone.setAnswerC(null);
                    break;
                case "D":
                    clone.setAnswerD(null);
                    break;
            }
        }
        return clone;
    }

    public AnswerResult handleAnswer(String roomCode, AnswerMessage message) {
        Long questionId = questionRedisService.getCurrentQuestionId(roomCode);
        String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
        QuestionResponse question = questionRedisService.getQuestionById(quizId, questionId);

        // Lấy clientSessionId từ player
        Player player = playerRepository.findBySessionId(message.getSessionId()).orElse(null);
        if (player == null) {
            return null;
        }
        String clientSessionId = player.getClientSessionId();

        List<TemporaryAnswer> tempAnswers = playerRedisService.getTemporaryAnswers(roomCode, clientSessionId);
        int baseTimeLimit = 10000;
        int baseScore = question.getScore() != null ? question.getScore() : 200;
        long timeTaken = message.getTimeTaken() != null ? message.getTimeTaken() : baseTimeLimit;
        int timeLimit = baseTimeLimit;

        SupportCardResult card = questionRedisService.getSupportCard(roomCode, message.getSessionId());
        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(message.getSelectedAnswer());
        int score = isCorrect ? calculateScore(timeTaken, timeLimit, baseScore) : 0;
        if (card != null && card.getQuestionId().equals(questionId)) {
            switch (card.getType()) {
                case DOUBLE_SCORE:
                    if (isCorrect)
                        score *= 2;
                    break;
                case HALF_SCORE:
                    if (isCorrect)
                        score /= 2;
                    break;
                case SKIP_QUESTION:
                    isCorrect = false;
                    score = 0;
                    break;
            }
            questionRedisService.removeSupportCard(roomCode, message.getSessionId());
        }

        TemporaryAnswer temp = new TemporaryAnswer(
                question.getId(),
                message.getSelectedAnswer(),
                isCorrect,
                score,
                timeTaken);
        playerRedisService.saveTemporaryAnswer(roomCode, clientSessionId, temp);

        AnswerResult result = new AnswerResult();
        result.setSessionId(message.getSessionId());
        result.setCorrect(isCorrect);
        result.setScore(score);

        return result;
    }

    private int calculateScore(long timeTakenMillis, int timeLimitMillis, int maxScore) {
        if (timeTakenMillis > timeLimitMillis) {
            return 0;
        }
        double ratio = (double) (timeLimitMillis - timeTakenMillis) / timeLimitMillis;
        ratio = Math.max(0.1, ratio);
        return (int) (maxScore * ratio);
    }

    public QuestionResponse sendNextQuestion(String roomCode) {
        String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
        int currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);

        String redisKey = "quiz:" + quizId + ":questions";
        Object cached = redisTemplate.opsForValue().get(redisKey);
        Long parsedQuizId = Long.parseLong(quizId);
        List<QuestionResponse> questions;

        if (cached != null && cached instanceof List<?>) {
            questions = ((List<?>) cached).stream()
                    .filter(obj -> obj instanceof LinkedHashMap)
                    .map(obj -> QuestionResponse.convertFromMap((LinkedHashMap<String, Object>) obj))
                    .collect(Collectors.toList());
        } else {
            List<Question> entities = questionRepository.findAllByQuiz_Id(parsedQuizId);
            questions = entities.stream()
                    .map(QuestionResponse::fromQuestionToQuestionResponse)
                    .collect(Collectors.toList());

            if (!questions.isEmpty()) {
                redisTemplate.opsForValue().set(redisKey, questions);
            }
        }

        if (currentIndex >= questions.size())
            return null;

        if (currentIndex > 0) {
            Long prevId = questions.get(currentIndex - 1).getId();
            Question prev = questionRepository.findById(prevId).orElse(null);
            if (prev != null) {
                handleUnansweredPlayers(roomCode, prev);
            }
        }

        QuestionResponse next = questions.get(currentIndex);
        questionRedisService.setCurrentQuestionId(roomCode, next.getId());
        questionRedisService.setCurrentQuestionIndex(roomCode, currentIndex + 1);
        return next;

    }

    public void handleUnansweredPlayers(String roomCode, Question question) {
        List<Player> players = playerRepository.findByRoom_RoomCode(roomCode);
        String redisKey = "answer:" + roomCode + ":" + question.getId();
        Map<Object, Object> submittedAnswers = redisTemplate.opsForHash().entries(redisKey);

        for (Player player : players) {
            String playerId = player.getId().toString();
            if (!submittedAnswers.containsKey(playerId)) {
                // Người này chưa trả lời => lưu câu trả lời 0 điểm
                Map<String, Object> unanswered = new HashMap<>();
                unanswered.put("playerId", player.getId());
                unanswered.put("selectedAnswer", null);
                unanswered.put("score", 0);
                unanswered.put("correct", false);
                unanswered.put("answeredAt", LocalDateTime.now());

                redisTemplate.opsForHash().put(redisKey, playerId, unanswered);
            }
        }
    }

    public void submitAllAnswers(String roomCode, String sessionId) {
        Player player = playerRepository.findByRoom_RoomCodeAndSessionId(roomCode, sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người chơi"));

        String clientSessionId = player.getClientSessionId();
        List<TemporaryAnswer> tempAnswers = playerRedisService.getTemporaryAnswers(roomCode, clientSessionId);

        for (TemporaryAnswer temp : tempAnswers) {
            Question question = questionRepository.findById(temp.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

            boolean alreadySaved = playerAnswerRepository.existsByPlayerAndQuestionAndSessionId(player, question,
                    sessionId);
            if (alreadySaved)
                continue;

            PlayerAnswer answer = new PlayerAnswer();
            answer.setPlayer(player);
            answer.setQuestion(question);
            answer.setSelectedAnswer(temp.getSelectedAnswer());
            answer.setCorrect(temp.isCorrect());
            answer.setAnsweredAt(LocalDateTime.now());
            answer.setScore(temp.getScore());
            answer.setSessionId(sessionId);

            playerAnswerRepository.save(answer);
        }
        playerRedisService.deleteTemporaryAnswers(roomCode, clientSessionId);
    }

    @SuppressWarnings("unchecked")
    public QuestionResponse startGameAndGetFirstQuestion(String roomCode, String quizId) {
        // questionRedisService.clearCachedQuestionsByQuizId(String.valueOf(quizId));
        String redisKey = "quiz:" + quizId + ":questions";
        questionRedisService.setQuizIdForRoomCode(roomCode, quizId);
        List<QuestionResponse> questions;
        Long parsedQuizId = Long.parseLong(quizId);
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null && cached instanceof List<?>) {
            questions = ((List<?>) cached).stream()
                    .filter(obj -> obj instanceof Map)
                    .map(obj -> QuestionResponse.convertFromMap((Map<String, Object>) obj))
                    .collect(Collectors.toList());
        } else {
            List<Question> entities = questionRepository.findAllByQuiz_Id(parsedQuizId);
            questions = entities.stream()
                    .map(QuestionResponse::fromQuestionToQuestionResponse)
                    .collect(Collectors.toList());

            if (!questions.isEmpty()) {
                redisTemplate.opsForValue().set(redisKey, questions);
            }
        }

        if (!questions.isEmpty()) {
            questionRedisService.setCurrentQuestionId(roomCode, questions.get(0).getId());
            questionRedisService.setCurrentQuestionIndex(roomCode, 1);
            return questions.get(0);
        }
        return null;
    }

    public void useSupportCard(String roomCode, String sessionId, SupportCardType type) {
        Long questionId = questionRedisService.getCurrentQuestionId(roomCode);
        SupportCardResult result = new SupportCardResult();
        result.setSessionId(sessionId);
        result.setType(type);
        result.setQuestionId(questionId);
        result.setMessage("Đã sử dụng thẻ: " + type.name());

        switch (type) {
            case HIDE_ANSWER:
                result.setEffectData(Map.of("Ẩn bớt câu hỏi", 2));
                break;
        }
        questionRedisService.saveSupportCard(roomCode, sessionId, result);
    }

    /**
     * Tạo thông tin timer cho câu hỏi
     */
    public QuestionTimerResponse createQuestionTimer(String roomCode, Long questionId) {
        // Lấy thời gian bắt đầu câu hỏi
        Long startTime = questionRedisService.getQuestionStartTime(roomCode, questionId);
        if (startTime == null) {
            startTime = System.currentTimeMillis();
            questionRedisService.setQuestionStartTime(roomCode, questionId);
        }

        return QuestionTimerResponse.builder()
                .questionId(questionId)
                .baseTimeLimit(10000) // 10 giây cơ bản
                .startTime(startTime)
                .message("Thời gian câu hỏi")
                .build();
    }

    public ReconnectResponse handleReconnect(String roomCode, String clientSessionId, String newSessionId) {
        if (roomCode == null || clientSessionId == null || newSessionId == null) {
            return ReconnectResponse.builder()
                    .success(false)
                    .message("Thông tin reconnect không hợp lệ")
                    .build();
        }

        boolean reconnected = playerService.reconnectPlayer(roomCode, clientSessionId, newSessionId);
        if (!reconnected) {
            return ReconnectResponse.builder()
                    .success(false)
                    .message("Không tìm thấy người chơi trong phòng")
                    .build();
        }

        // Lấy thông tin game hiện tại
        String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
        int currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(roomCode);

        if (quizId == null || currentQuestionId == null) {
            return ReconnectResponse.builder()
                    .success(false)
                    .message("Game chưa bắt đầu")
                    .build();
        }

        // Lấy danh sách câu hỏi
        List<QuestionResponse> questions = getQuestionsFromCache(quizId);
        if (questions.isEmpty()) {
            return ReconnectResponse.builder()
                    .success(false)
                    .message("Không tìm thấy câu hỏi")
                    .build();
        }

        // Kiểm tra bounds cho currentIndex
        if (currentIndex <= 0 || currentIndex > questions.size()) {
            // Nếu currentIndex > questions.size(), có thể game đã kết thúc
            if (currentIndex > questions.size()) {
                // Game đã kết thúc, chỉ trả về thông tin điểm số
                int finalScore = calculateCurrentScore(roomCode, clientSessionId);
                return ReconnectResponse.builder()
                        .success(true)
                        .message("Game đã kết thúc")
                        .currentScore(finalScore)
                        .currentQuestionIndex(questions.size())
                        .totalQuestions(questions.size())
                        .hasAnsweredCurrentQuestion(true)
                        .build();
            } else {
                return ReconnectResponse.builder()
                        .success(false)
                        .message("Chỉ số câu hỏi không hợp lệ")
                        .build();
            }
        }

        handleUnansweredPreviousQuestions(roomCode, clientSessionId, currentIndex, questions);
        QuestionResponse currentQuestion = questions.get(currentIndex - 1);
        boolean hasAnsweredCurrent = playerRedisService.hasAnsweredCurrentQuestion(roomCode, clientSessionId,
                currentQuestionId);
        int currentScore = calculateCurrentScore(roomCode, clientSessionId);

        ReconnectResponse response = ReconnectResponse.builder()
                .success(true)
                .message("Kết nối lại thành công")
                .currentQuestion(currentQuestion)
                .currentScore(currentScore)
                .currentQuestionIndex(currentIndex)
                .totalQuestions(questions.size())
                .hasAnsweredCurrentQuestion(hasAnsweredCurrent)
                .build();

        // Nếu đã trả lời câu hỏi hiện tại, lấy kết quả
        if (hasAnsweredCurrent) {
            AnswerResult lastResult = playerRedisService.getLastAnswerResult(roomCode, clientSessionId);
            response.setLastAnswerResult(lastResult);
        }

        return response;
    }

    private void handleUnansweredPreviousQuestions(String roomCode, String clientSessionId, int currentIndex,
            List<QuestionResponse> questions) {
        // Xử lý các câu hỏi từ 0 đến currentIndex - 2 (câu hỏi trước câu hiện tại)
        // Nếu currentIndex = 1 (câu hỏi đầu tiên), không có câu hỏi nào trước đó cần xử
        // lý
        if (currentIndex <= 1) {
            return;
        }

        for (int i = 0; i < currentIndex - 1; i++) {
            if (i >= questions.size()) {
                break; // Đảm bảo không vượt quá bounds
            }

            QuestionResponse question = questions.get(i);
            boolean hasAnswered = playerRedisService.hasAnsweredQuestion(roomCode, clientSessionId, question.getId());

            if (!hasAnswered) {
                // Lưu kết quả 0 điểm cho câu hỏi này
                TemporaryAnswer tempAnswer = new TemporaryAnswer(
                        question.getId(),
                        null, // không trả lời
                        false, // sai
                        0, // 0 điểm
                        0L // thời gian 0
                );
                playerRedisService.saveTemporaryAnswer(roomCode, clientSessionId, tempAnswer);
            }
        }
    }

    private int calculateCurrentScore(String roomCode, String clientSessionId) {
        List<TemporaryAnswer> tempAnswers = playerRedisService.getTemporaryAnswers(roomCode, clientSessionId);
        return tempAnswers.stream()
                .mapToInt(TemporaryAnswer::getScore)
                .sum();
    }

    private List<QuestionResponse> getQuestionsFromCache(String quizId) {
        String redisKey = "quiz:" + quizId + ":questions";
        Object cached = redisTemplate.opsForValue().get(redisKey);
        Long parsedQuizId = Long.parseLong(quizId);

        if (cached != null && cached instanceof List<?>) {
            return ((List<?>) cached).stream()
                    .filter(obj -> obj instanceof LinkedHashMap)
                    .map(obj -> QuestionResponse.convertFromMap((LinkedHashMap<String, Object>) obj))
                    .collect(Collectors.toList());
        } else {
            List<Question> entities = questionRepository.findAllByQuiz_Id(parsedQuizId);
            List<QuestionResponse> questions = entities.stream()
                    .map(QuestionResponse::fromQuestionToQuestionResponse)
                    .collect(Collectors.toList());

            if (!questions.isEmpty()) {
                redisTemplate.opsForValue().set(redisKey, questions);
            }
            return questions;
        }
    }

    /**
     * Tính toán và trả về bảng xếp hạng cho phòng
     */
    public LeaderboardResponse getLeaderboard(String roomCode) {
        String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
        int currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);

        if (quizId == null) {
            return LeaderboardResponse.builder()
                    .roomCode(roomCode)
                    .players(List.of())
                    .currentQuestionIndex(0)
                    .totalQuestions(0)
                    .build();
        }

        List<QuestionResponse> questions = getQuestionsFromCache(quizId);
        List<Player> players = playerRepository.findByRoom_RoomCode(roomCode);

        List<PlayerScoreResponse> playerScores = players.stream()
                .map(player -> calculatePlayerScore(roomCode, player, questions))
                .sorted((p1, p2) -> Integer.compare(p2.getTotalScore(), p1.getTotalScore())) // Sắp xếp theo điểm giảm
                                                                                             // dần
                .collect(Collectors.toList());

        return LeaderboardResponse.builder()
                .roomCode(roomCode)
                .players(playerScores)
                .currentQuestionIndex(currentIndex)
                .totalQuestions(questions.size())
                .build();
    }

    /**
     * Tính toán điểm số cho một người chơi
     */
    private PlayerScoreResponse calculatePlayerScore(String roomCode, Player player, List<QuestionResponse> questions) {
        List<TemporaryAnswer> tempAnswers = playerRedisService.getTemporaryAnswers(roomCode,
                player.getClientSessionId());

        int totalScore = tempAnswers.stream()
                .mapToInt(TemporaryAnswer::getScore)
                .sum();

        int correctAnswers = (int) tempAnswers.stream()
                .filter(TemporaryAnswer::isCorrect)
                .count();

        int totalQuestions = questions.size();
        double accuracy = totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0.0;

        return PlayerScoreResponse.builder()
                .playerName(player.getName())
                .clientSessionId(player.getClientSessionId())
                .totalScore(totalScore)
                .correctAnswers(correctAnswers)
                .totalQuestions(totalQuestions)
                .accuracy(accuracy)
                .build();
    }

}
