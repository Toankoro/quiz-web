package com.example.quizgame.entity;

import com.example.quizgame.dto.QuestionListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@EntityListeners(QuestionListener.class)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;
    
    @NotBlank(message = "Chưa chọn đáp án đúng")
    private String correctAnswer;
    private Integer limitedTime = 10;
    private Integer score;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;
}
