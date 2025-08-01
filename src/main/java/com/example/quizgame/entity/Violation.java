package com.example.quizgame.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private int count;
    private String level;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}
