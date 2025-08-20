package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByUserAndRoomStartedAtIsNull(User user);
    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);
    void deleteAllByRoomId(Long roomId);
    Optional<RoomParticipant> findByUserId(Long userId);
    Optional<RoomParticipant> findByRoomIdAndUserIdAndRoom_StartedAtIsNull(Long roomId, Long userId);
    Optional<List<RoomParticipant>> findByRoom_PinCode (String pinCode);
    Optional<RoomParticipant> findByRoom_PinCodeAndUser_Username(String pinCode, String username);
    Optional<RoomParticipant> findByRoom_PinCodeAndClientSessionId(String pinCode, String clientSessionId);
    Optional<RoomParticipant> findByRoom_PinCodeAndUser_Id(String pinCode, Long userId);
}
