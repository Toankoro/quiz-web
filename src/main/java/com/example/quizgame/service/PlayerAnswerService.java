package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.dto.answer.HistoryDetailDTO;
import com.example.quizgame.dto.answer.HistorySummaryDTO;
import com.example.quizgame.dto.answer.PlayHistoryDTO;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.entity.*;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerAnswerService {

    private final PlayerAnswerRepository playerAnswerRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveAnswersFromHistory(String pinCode, String clientSessionId, List<AnswerResult> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        RoomParticipant participant = roomParticipantRepository
                .findByRoom_PinCodeAndClientSessionId(pinCode, clientSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người chơi trong phòng"));

        // Kiểm tra xem đã có dữ liệu lưu cho session này chưa
        Long existingCount = playerAnswerRepository.countByUser_IdAndRoom_IdAndClientSessionId(
                participant.getUser().getId(),
                participant.getRoom().getId(),
                clientSessionId);

        if (existingCount > 0) {
            System.out.println("Dữ liệu đã được lưu trước đó cho session: " + clientSessionId);
            return; // Đã có dữ liệu, không lưu nữa
        }

        List<Long> questionIds = history.stream()
                .map(AnswerResult::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Long quizId = participant.getRoom().getQuiz().getId();
        String redisKey = "quiz:" + quizId + ":questions";

        Map<Long, Question> questionMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        for (Long qId : questionIds) {
            Object cached = redisTemplate.opsForHash().get(redisKey, qId.toString());
            if (cached != null) {
                QuestionResponse qr = objectMapper.convertValue(cached, QuestionResponse.class);
                questionMap.put(qId, convertToQuestionEntity(qr));
            } else {
                missingIds.add(qId);
            }
        }

        if (!missingIds.isEmpty()) {
            questionRepository.findAllById(missingIds)
                    .forEach(q -> questionMap.put(q.getId(), q));
        }

        List<PlayerAnswer> answersToSave = history.stream()
                .map(result -> {
                    Question question = questionMap.get(result.getQuestionId());
                    if (question == null)
                        return null;
                    return PlayerAnswer.builder()
                            .user(participant.getUser())
                            .room(participant.getRoom())
                            .clientSessionId(clientSessionId)
                            .question(question)
                            .selectedAnswer(result.getSelectedAnswer())
                            .correct(result.isCorrect())
                            .score(result.getScore())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!answersToSave.isEmpty()) {
            try {
                playerAnswerRepository.saveAll(answersToSave);
                System.out.println(
                        "Đã lưu thành công " + answersToSave.size() + " câu trả lời cho session: " + clientSessionId);
            } catch (Exception e) {
                System.err.println("Lỗi khi lưu PlayerAnswer: " + e.getMessage());
                // Có thể do constraint violation, log và bỏ qua
                if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                    System.out.println("Dữ liệu đã tồn tại, bỏ qua lưu duplicate.");
                } else {
                    throw e; // Re-throw nếu không phải lỗi duplicate
                }
            }
        }
    }

    private Question convertToQuestionEntity(QuestionResponse qr) {
        if (qr == null)
            return null;
        return Question.builder()
                .id(qr.getId())
                .content(qr.getContent())
                .answerA(qr.getAnswerA())
                .answerB(qr.getAnswerB())
                .answerC(qr.getAnswerC())
                .answerD(qr.getAnswerD())
                .correctAnswer(qr.getCorrectAnswer())
                .score(qr.getScore())
                .limitedTime(qr.getLimitedTime())
                .build();
    }

    public List<HistorySummaryDTO> getHistorySummary(Long userId) {
        // Sử dụng liên kết trực tiếp với User thay vì thông qua RoomParticipant
        List<PlayerAnswer> answers = playerAnswerRepository.findByUser_Id(userId);

        return answers.stream()
                .collect(Collectors.groupingBy(pa -> pa.getRoom().getId()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long roomId = entry.getKey();
                    List<PlayerAnswer> playerAnswers = entry.getValue();

                    Room room = playerAnswers.get(0).getRoom();
                    Quiz quiz = room.getQuiz();

                    return HistorySummaryDTO.builder()
                            .roomId(roomId)
                            .quizTitle(quiz.getTopic())
                            .lessonName(quiz.getDescription())
                            .questionCount(playerAnswers.size())
                            .totalScore(
                                    playerAnswers.stream().mapToInt(a -> a.getScore() != null ? a.getScore() : 0).sum())
                            .playedAt(room.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<HistoryDetailDTO> getHistoryDetail(Long userId, Long roomId) {
        // Sử dụng liên kết trực tiếp với User và Room
        List<PlayerAnswer> answers = playerAnswerRepository
                .findByUser_IdAndRoom_Id(userId, roomId);

        return answers.stream()
                .map(pa -> HistoryDetailDTO.builder()
                        .questionId(pa.getQuestion().getId())
                        .questionContent(pa.getQuestion().getContent())
                        .answerA(pa.getQuestion().getAnswerA())
                        .answerB(pa.getQuestion().getAnswerB())
                        .answerC(pa.getQuestion().getAnswerC())
                        .answerD(pa.getQuestion().getAnswerD())
                        .selectedAnswer(pa.getSelectedAnswer())
                        .correct(pa.isCorrect())
                        .score(pa.getScore())
                        .correctAnswer(pa.getQuestion().getCorrectAnswer())
                        .explanation(pa.getQuestion().getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUserHistory(Long userId, Long roomId) {
        // Sử dụng liên kết trực tiếp với User và Room
        playerAnswerRepository.deleteByUser_IdAndRoom_Id(userId, roomId);
    }

    /**
     * Method để migrate dữ liệu cũ từ database nếu có dữ liệu thiếu user hoặc room
     * Lưu ý: Method này chỉ cần thiết nếu có dữ liệu cũ trong database
     */
    @Transactional
    public void migrateExistingData() {
        // Kiểm tra xem có PlayerAnswer nào thiếu user hoặc room không
        long countMissingData = playerAnswerRepository.findAll()
                .stream()
                .filter(pa -> pa.getUser() == null || pa.getRoom() == null)
                .count();

        if (countMissingData > 0) {
            System.out.println(
                    "Cảnh báo: Có " + countMissingData + " bản ghi PlayerAnswer thiếu thông tin user hoặc room.");
            System.out.println("Cần xử lý dữ liệu này thủ công hoặc xóa bỏ.");
        } else {
            System.out.println("Tất cả PlayerAnswer đã có đầy đủ thông tin user và room.");
        }
    }

    public List<PlayHistoryDTO> getPlayHistory(Long userId, String name, LocalDate date) {
        if (date != null) {
            return playerAnswerRepository.findHistoryWithDate(userId, name, date);
        } else {
            return playerAnswerRepository.findHistoryNoDate(userId, name);
        }
    }

    // Xóa lịch sử chơi của user trong một room
    @Transactional
    public void deleteHistoryByRoom(Long userId, Long roomId) {
        playerAnswerRepository.deleteByUserAndRoom(userId, roomId);
    }

}
