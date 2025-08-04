package com.example.quizgame.dto;

import com.example.quizgame.entity.Question;
import com.example.quizgame.entity.RoomParticipant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class PlayerAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_participant_id")
    private RoomParticipant roomParticipant;

    private String sessionId;

    @ManyToOne
    private Question question;

    private String selectedAnswer;

    private boolean correct;

    private LocalDateTime answeredAt = LocalDateTime.now();

    private Integer score;

}
