package com.example.quizgame.dto.response;

import com.example.quizgame.entity.Question;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private String content;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    private String imageUrl;
    private String correctAnswer;
    private Integer limitedTime;
    private Integer score;
    public static QuestionResponse fromQuestionToQuestionResponse(Question question) {
       return QuestionResponse.builder()
               .id(question.getId())
               .content(question.getContent())
               .answerA(question.getAnswerA())
               .answerB(question.getAnswerB())
               .answerC(question.getAnswerC())
               .answerD(question.getAnswerD())
               .score(question.getScore())
               .limitedTime(question.getLimitedTime() != null ? question.getLimitedTime() : 20)
               .correctAnswer(question.getCorrectAnswer())
               .build();
    }

    public static QuestionResponse convertFromMap(Map<String, Object> map) {
        return QuestionResponse.builder()
                .id(Long.valueOf(map.get("id").toString()))
                .content((String) map.get("content"))
                .answerA((String) map.get("answerA"))
                .answerB((String) map.get("answerB"))
                .answerC((String) map.get("answerC"))
                .answerD((String) map.get("answerD"))
                .imageUrl((String) map.get("imageUrl"))
                .score((Integer) map.get("score"))
                .limitedTime((Integer) map.get("limitedTime"))
                .correctAnswer((String) map.get("correctAnswer"))
                .build();
    }


}
