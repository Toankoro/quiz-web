package com.example.quizgame.dto;

import com.example.quizgame.entity.Player;
import com.example.quizgame.entity.Question;
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
    private Player player;

    @ManyToOne
    private Question question;

    private String selectedAnswer;

    private boolean correct;

    private LocalDateTime answeredAt = LocalDateTime.now();

    private Integer score;

    private String sessionId;
}
