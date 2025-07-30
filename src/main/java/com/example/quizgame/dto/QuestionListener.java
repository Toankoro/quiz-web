package com.example.quizgame.dto;

import com.example.quizgame.config.BeanUtil;
import com.example.quizgame.entity.Question;
import com.example.quizgame.service.QuestionRedisService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionListener {

    private static final Logger logger = LoggerFactory.getLogger(QuestionListener.class);

    @PostPersist
    @PostUpdate
    @PostRemove
    public void clearCache(Question question) {
        if (question != null && question.getQuiz() != null) {
            Long quizId = question.getQuiz().getId();
            logger.info("Clearing Redis cache for quizId={}", quizId);
            QuestionRedisService questionRedisService = BeanUtil.getBean(QuestionRedisService.class);
            questionRedisService.clearCachedQuestionsByQuizId(String.valueOf(quizId));
        }
    }
}


