package com.example.quizgame.dto.question;

import com.example.quizgame.entity.Question;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionResponseToParticipant {
    private Long id;
    private String content;
    private String description;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    private String imageUrl;
    private Integer limitedTime;
    private Integer score;
    private boolean questionLast;
    private long startTime;
    private Integer currentQuestion;
    private Integer totalQuestion;

    public static QuestionResponseToParticipant fromQuestionResponseToQuestionResponseToParticipant(QuestionResponse questionResponse, boolean questionLast, Integer currentQuestion, Integer totalQuestion) {
        return QuestionResponseToParticipant.builder()
                .id(questionResponse.getId())
                .content(questionResponse.getContent())
                .description(questionResponse.getDescription())
                .answerA(questionResponse.getAnswerA())
                .answerB(questionResponse.getAnswerB())
                .answerC(questionResponse.getAnswerC())
                .answerD(questionResponse.getAnswerD())
                .imageUrl(questionResponse.getImageUrl())
                .score(questionResponse.getScore())
                .limitedTime(questionResponse.getLimitedTime() != null ? questionResponse.getLimitedTime() : 20)
                .questionLast(questionLast)
                .startTime(System.currentTimeMillis())
                .currentQuestion(currentQuestion)
                .totalQuestion(totalQuestion)
                .build();
    }
}
