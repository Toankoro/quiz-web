package com.example.quizgame.service;

import com.example.quizgame.dto.answer.PlayerGameInfoDTO;
import com.example.quizgame.entity.GameRanking;
import com.example.quizgame.entity.PlayerAnswer;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.GameRankingRepository;
import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlayerGameService {

    private final PlayerAnswerRepository playerAnswerRepo;
    private final GameRankingRepository gameRankingRepo;

    public PlayerGameService(PlayerAnswerRepository playerAnswerRepo, GameRankingRepository gameRankingRepo) {
        this.playerAnswerRepo = playerAnswerRepo;
        this.gameRankingRepo = gameRankingRepo;
    }

    public PlayerGameInfoDTO getPlayerGameInfo(Long roomId, Long userId) {
        // Lấy tất cả câu trả lời của user trong room
        List<PlayerAnswer> answers = playerAnswerRepo.findByUser_IdAndRoom_Id(userId,roomId);
        if (answers.isEmpty()) return null;

        // Lấy thông tin ranking
        GameRanking ranking = gameRankingRepo.findByRoom_IdAndUser_Id(roomId, userId);

        // Lấy thông tin room và user từ câu trả lời đầu tiên
        Room room = answers.get(0).getRoom();
        User user = answers.get(0).getUser();

        // Tính số câu đúng và sai
        int correctCount = (int) answers.stream().filter(PlayerAnswer::isCorrect).count();
        int wrongCount = answers.size() - correctCount;

        // Tạo DTO trả về
        PlayerGameInfoDTO dto = new PlayerGameInfoDTO();
        dto.setTopicName(room.getQuiz().getTopic());
        dto.setQuizName(room.getQuiz().getName());
        dto.setStartedAt(room.getStartedAt());
        dto.setPlayerName(user.getFirstname());
        if (ranking != null) {
            dto.setRanking(ranking.getRanking());
            dto.setScore(ranking.getScore());
            dto.setCorrectCount(ranking.getCorrectCount());
        }
        dto.setWrongCount(wrongCount);
        dto.setTotalQuestions(answers.size());

        return dto;
    }
}
