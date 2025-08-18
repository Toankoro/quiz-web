package com.example.quizgame.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_participant_id")
    private RoomParticipant roomParticipant;

    private String clientSessionId;

    @ManyToOne
    private Question question;

    private String selectedAnswer;

    private boolean correct;

    private LocalDateTime answeredAt;

    private Integer score;

    @PrePersist
    public void onCreate() {
        this.answeredAt = LocalDateTime.now();
    }

}
