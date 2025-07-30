package com.example.quizgame.entity;

import com.example.quizgame.dto.RoomState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class GameRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomCode;

    private String hostUsername;

    private String hostClientSessionId;

    @Enumerated(EnumType.STRING)
    private RoomState roomState;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    private LocalDateTime createdAt = LocalDateTime.now();
}
