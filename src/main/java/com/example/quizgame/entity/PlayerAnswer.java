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
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "room_id", "question_id", "client_session_id" })
})
public class PlayerAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

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
