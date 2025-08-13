package com.example.quizgame.service;

import com.example.quizgame.dto.rank.GameRankingDTO;
import com.example.quizgame.entity.GameRanking;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.GameRankingRepository;
import com.example.quizgame.reponsitory.RoomParticipantRepository;
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
    private final RoomParticipantRepository roomParticipantRepo;

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
        if (scoreToAdd > 0) {
            ranking.setCorrectCount(ranking.getCorrectCount() + 1);
        }

        rankingRepo.save(ranking);
        updateRanksAndBroadcast(room);
    }

    public void updateRanksAndBroadcast(Room room) {
        List<GameRanking> rankings = rankingRepo.findByRoomOrderByScoreDescWithUser(room);

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRanking(i + 1);
        }
        rankingRepo.saveAll(rankings);

        List<GameRankingDTO> dtoList = rankings.stream()
                .map(r -> {
                    Optional<RoomParticipant> pOpt = roomParticipantRepo.findByRoomIdAndUserId(room.getId(), r.getUser().getId());
                    String avatar = pOpt.map(RoomParticipant::getAvatar).orElse(null);
                    return GameRankingDTO.from(r, avatar);
                })
                .collect(Collectors.toList());

        kafkaTemplate.send("room-rankings", room.getId().toString(), dtoList);
    }

    public List<GameRankingDTO> getRanking(Room room) {
        return rankingRepo.findByRoomOrderByScoreDescWithUser(room)
                .stream()
                .map(r -> {
                    RoomParticipant p = roomParticipantRepo
                            .findByRoomIdAndUserId(room.getId(), r.getUser().getId())
                            .orElse(null);
                    String avatar = p != null ? p.getAvatar() : null;
                    return GameRankingDTO.from(r, avatar);
                })
                .collect(Collectors.toList());
    }
}