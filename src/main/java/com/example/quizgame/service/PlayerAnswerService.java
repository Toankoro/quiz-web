package com.example.quizgame.service;

import com.example.quizgame.dto.answer.AnswerResult;
import com.example.quizgame.entity.PlayerAnswer;
import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerAnswerService {

    private final PlayerAnswerRepository playerAnswerRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final QuestionRepository questionRepository;

    public void saveAnswersFromHistory(String pinCode, String clientSessionId, List<AnswerResult> history) {
        RoomParticipant participant = roomParticipantRepository
                .findByRoom_PinCodeAndClientSessionId(pinCode, clientSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người chơi trong phòng"));
        List<PlayerAnswer> answers = history.stream().map(result -> {
            Question question = questionRepository.findById(result.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));
            PlayerAnswer pa = new PlayerAnswer();
            pa.setRoomParticipant(participant);
            pa.setSessionId(clientSessionId);
            pa.setQuestion(question);
            pa.setSelectedAnswer(result.getSelectedAnswer());
            pa.setCorrect(result.isCorrect());
            pa.setScore(result.getScore());
            return pa;
        }).collect(Collectors.toList());

        playerAnswerRepository.saveAll(answers);
    }
}
