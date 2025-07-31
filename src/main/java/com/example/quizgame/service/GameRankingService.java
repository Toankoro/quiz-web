package com.example.quizgame.service;

import com.example.quizgame.dto.rank.GameRankingDTO;
import com.example.quizgame.entity.GameRanking;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.GameRankingRepository;
import com.example.quizgame.reponsitory.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameRankingService {

    private final GameRankingRepository rankingRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepository userRepo;

    // Cộng điểm và cập nhật xếp hạng
    @Transactional
    public void addScoreAndCorrect(Room room, User user, int scoreToAdd) {
        Optional<GameRanking> optional = rankingRepo.findByRoomAndUser(room, user);
        if (optional.isEmpty()) {
            // Không tạo mới nữa nếu không tồn tại
            return;
        }

        GameRanking ranking = optional.get();
        ranking.setScore(ranking.getScore() + scoreToAdd);
        if(scoreToAdd>0){
            ranking.setCorrectCount(ranking.getCorrectCount() + 1);// tăng số câu đúng
        }
        rankingRepo.save(ranking);

        updateRanksAndBroadcast(room);
    }



    // Gửi WebSocket ranking qua Kafka
    public void updateRanksAndBroadcast(Room room) {
        List<GameRanking> rankings = rankingRepo.findByRoomOrderByScoreDescWithUser(room);
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRanking(i + 1);
        }
        rankingRepo.saveAll(rankings);

        List<GameRankingDTO> dtoList = rankings.stream()
                .map(GameRankingDTO::from)
                .collect(Collectors.toList());

        kafkaTemplate.send("room-rankings", room.getId().toString(), dtoList);
    }

    // Truy xuất bảng xếp hạng hiện tại
    public List<GameRankingDTO> getRanking(Room room) {
        return rankingRepo.findByRoomOrderByScoreDescWithUser(room)
                .stream()
                .map(GameRankingDTO::from)
                .collect(Collectors.toList());
    }
}
