package com.example.quizgame.dto.question;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionRequest {
    private String content;
    private String description;
    @NotBlank(message = "Câu trả lời không được bỏ trống")
    private String answerA;
    @NotBlank(message = "Câu trả lời không được bỏ trống")
    private String answerB;
    @NotBlank(message = "Câu trả lời không được bỏ trống")
    private String answerC;
    @NotBlank(message = "Câu trả lời không được bỏ trống")
    private String answerD;
    private String imageUrl;
    @NotBlank(message = "Đáp án không được bỏ trống !")
    private String correctAnswer;
    private Integer limitedTime;
    private Integer score;
    public Long quizId;
}
