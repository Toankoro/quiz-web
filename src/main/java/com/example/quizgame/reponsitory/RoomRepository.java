package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByPinCodeAndStartedAtIsNull(String pin);
    Optional<Room> findByPinCode(String pin);

}

