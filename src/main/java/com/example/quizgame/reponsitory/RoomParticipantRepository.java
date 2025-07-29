package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByUserAndRoomStartedAtIsNull(User user);
    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);
    void deleteAllByRoomId(Long roomId);
    Optional<RoomParticipant> findByRoomIdAndUserIdAndRoom_StartedAtIsNull(Long roomId, Long userId);
}
