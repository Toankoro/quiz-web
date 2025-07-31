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

    public void resetRoomState(String roomCode, String quizId) {
        // Xóa điểm, câu trả lời tạm, support card, v.v.
        playerRedisService.deleteAllTemporaryAnswers(roomCode);
        questionRedisService.clearAllSupportCards(roomCode);
        // Reset chỉ số câu hỏi, current question, quizId
        questionRedisService.setQuizIdForRoomCode(roomCode, quizId);
        questionRedisService.setCurrentQuestionIndex(roomCode, 0);
        questionRedisService.setCurrentQuestionId(roomCode, null);
        playerRedisService.deleteAllTemporaryAnswers(roomCode);
        playerRedisService.getPlayersInRoom(roomCode)
                .forEach(player -> playerRedisService.deleteTemporaryAnswers(roomCode, player.getClientSessionId()));
        // Xóa cache câu hỏi nếu cần
//        questionRedisService.clearCachedQuestionsByQuizId(quizId);
    }

    // lấy sessionId từ
    public List<String> getSessionIdsInRoom(String roomCode) {
        return playerRepository.findByRoom_RoomCode(roomCode)
                .stream()
                // .map(Player::getSessionId)
                .map(Player::getClientSessionId)
                .collect(Collectors.toList());
    }

    public boolean hasHideAnswerCard(String roomCode, String sessionId, Long questionId) {
        SupportCardResult card = questionRedisService.getSupportCard(roomCode, sessionId);
        return card != null
                && card.getType() == SupportCardType.HIDE_ANSWER
                && card.getQuestionId().equals(questionId);
    }


     // kiểm tra xem người chơi có thể sử dụng support card hay không

    public boolean canUseSupportCard(String roomCode, String sessionId) {
        Long currentQuestionId = questionRedisService.getCurrentQuestionId(roomCode);
        if (currentQuestionId == null) {
            return false;
        }

        SupportCardResult existingCard = questionRedisService.getSupportCard(roomCode, sessionId);
        return existingCard == null || !existingCard.getQuestionId().equals(currentQuestionId);
    }

    public QuestionResponse hideTwoAnswers(QuestionResponse question) {
        if (question == null) {
            return null;
        }

        // Tạo bản sao của câu hỏi
        QuestionResponse clone = QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .answerA(question.getAnswerA())
                .answerB(question.getAnswerB())
                .answerC(question.getAnswerC())
                .answerD(question.getAnswerD())
                .imageUrl(question.getImageUrl())
                .correctAnswer(question.getCorrectAnswer())
                .limitedTime(question.getLimitedTime())
                .score(question.getScore())
                .build();

        String correct = question.getCorrectAnswer();
        if (correct == null || correct.trim().isEmpty()) {
            return clone; // Không thể ẩn đáp án nếu không có đáp án đúng
        }

        // Lấy danh sách các đáp án sai
        List<String> answers = List.of("A", "B", "C", "D");
        List<String> wrongs = answers.stream()
                .filter(a -> !a.equalsIgnoreCase(correct))
                .collect(Collectors.toList());

        // Nếu chỉ có 1 đáp án sai, chỉ ẩn 1 đáp án
        int answersToHide = Math.min(2, wrongs.size());
        if (answersToHide == 0) {
            return clone; // Không có đáp án sai để ẩn
        }

        Collections.shuffle(wrongs);
        List<String> toHide = wrongs.subList(0, answersToHide);

        // Ẩn các đáp án sai
        for (String ans : toHide) {
            switch (ans.toUpperCase()) {
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

        if (question == null) {
            return null;
        }

        // Tìm người chơi theo clientSessionId
        Player player = playerRepository.findByClientSessionId(message.getClientSessionId()).orElse(null);
        if (player == null) {
            return null;
        }
        String clientSessionId = player.getClientSessionId();

        List<TemporaryAnswer> tempAnswers = playerRedisService.getTemporaryAnswers(roomCode, clientSessionId);
        int baseTimeLimit = 10000;
        int baseScore = question.getScore() != null ? question.getScore() : 200;
        long timeTaken = message.getTimeTaken() != null ? message.getTimeTaken() : baseTimeLimit;
        int timeLimit = baseTimeLimit;

        // Kiểm tra xem người chơi có sử dụng support card không
        SupportCardResult card = questionRedisService.getSupportCard(roomCode, message.getClientSessionId());
        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(message.getSelectedAnswer());
        int score = isCorrect ? calculateScore(timeTaken, timeLimit, baseScore) : 0;

        if (card != null && card.getQuestionId().equals(questionId)) {
            switch (card.getType()) {
                case DOUBLE_SCORE:
                    if (isCorrect) {
                        score *= 2;
                    }
                    break;
                case HALF_SCORE:
                    if (isCorrect) {
                        score = (int) (score * 0.5);
                    }
                    break;
                case SKIP_QUESTION:
                    isCorrect = false;
                    score = 0;
                    break;
                case HIDE_ANSWER:
                    break;
            }
            // Xóa card sau khi sử dụng
            questionRedisService.removeSupportCard(roomCode, message.getClientSessionId());
        }

        TemporaryAnswer temp = new TemporaryAnswer(
                question.getId(),
                message.getSelectedAnswer(),
                isCorrect,
                score,
                timeTaken);
        playerRedisService.saveTemporaryAnswer(roomCode, clientSessionId, temp);

        AnswerResult result = new AnswerResult();
        result.setClientSessionId(message.getClientSessionId());
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
        Integer currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);
        if (currentIndex == null) {
            currentIndex = 0;
            questionRedisService.setCurrentQuestionIndex(roomCode, 0);
        }
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

        // Xóa tất cả support cards khi chuyển sang câu hỏi mới
        clearAllSupportCards(roomCode);

        QuestionResponse next = questions.get(currentIndex);
        questionRedisService.setCurrentQuestionId(roomCode, next.getId());
        questionRedisService.setCurrentQuestionIndex(roomCode, currentIndex + 1);
        return next;

    }

    public QuestionResponse getCurrentQuestion(String roomCode) {
        String quizId = questionRedisService.getQuizIdByRoomCode(roomCode);
        Integer currentIndex = questionRedisService.getCurrentQuestionIndex(roomCode);
        if (currentIndex == null) {
            currentIndex = 0;
            questionRedisService.setCurrentQuestionIndex(roomCode, 0);
        }
        // get question save in cache
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

        }
        return questions.get(currentIndex);

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
        if (questionId == null) {
            return; // Không có câu hỏi hiện tại
        }

        // Kiểm tra xem người chơi đã sử dụng card cho câu hỏi này chưa
        SupportCardResult existingCard = questionRedisService.getSupportCard(roomCode, sessionId);
        if (existingCard != null && existingCard.getQuestionId().equals(questionId)) {
            // Người chơi đã sử dụng card cho câu hỏi này rồi
            return;
        }

        SupportCardResult result = new SupportCardResult();
        result.setSessionId(sessionId);
        result.setType(type);
        result.setQuestionId(questionId);
        result.setMessage("Đã sử dụng thẻ: " + type.name());

        switch (type) {
            case HIDE_ANSWER:
                result.setEffectData(Map.of("Ẩn bớt câu hỏi", 2));
                break;
            case DOUBLE_SCORE:
                result.setEffectData(Map.of("Nhân đôi điểm", 2));
                break;
            case HALF_SCORE:
                result.setEffectData(Map.of("Giảm một nửa điểm", 0.5));
                break;
            case SKIP_QUESTION:
                result.setEffectData(Map.of("Bỏ qua câu hỏi", "skip"));
                break;
        }
        questionRedisService.saveSupportCard(roomCode, sessionId, result);
    }

    /**
     * Xóa tất cả support cards trong phòng khi chuyển sang câu hỏi mới
     */
    private void clearAllSupportCards(String roomCode) {
        questionRedisService.clearAllSupportCards(roomCode);
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

        // Kiểm tra xem người chơi có sử dụng card ẩn đáp án cho câu hỏi hiện tại không
        boolean hasHideAnswerCard = hasHideAnswerCard(roomCode, clientSessionId, currentQuestionId);
        if (hasHideAnswerCard && !hasAnsweredCurrent) {
            // Nếu đã sử dụng card ẩn đáp án và chưa trả lời, trả về câu hỏi đã ẩn đáp án
            currentQuestion = hideTwoAnswers(currentQuestion);
        }

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

    // Lấy tất cả câu hỏi từ cache
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
        // lấy danh sách câu hỏi
        List<QuestionResponse> questions = getQuestionsFromCache(quizId);
        List<Player> players = playerRepository.findByRoom_RoomCode(roomCode);

        List<PlayerScoreResponse> playerScores = players.stream()
                .map(player -> calculatePlayerScore(roomCode, player, questions))
                .sorted((p1, p2) -> Integer.compare(p2.getTotalScore(), p1.getTotalScore()))
                .collect(Collectors.toList());

        return LeaderboardResponse.builder()
                .roomCode(roomCode)
                .players(playerScores)
                .currentQuestionIndex(currentIndex)
                .totalQuestions(questions.size())
                .build();
    }
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
