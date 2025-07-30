package com.example.quizgame.entity;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class PlayerListenner {
    private static final Logger logger = LoggerFactory.getLogger(PlayerListenner.class);

    @PrePersist
    public void prePersist (Player player) {
        logger.info("pre persist player");
    }

    @PostPersist
    public void postPersist (Player player) {
        logger.info("post persist player");

    }
}
