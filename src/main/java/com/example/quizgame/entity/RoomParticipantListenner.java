package com.example.quizgame.entity;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class RoomParticipantListenner {
    private static final Logger logger = LoggerFactory.getLogger(RoomParticipantListenner.class);

    @PrePersist
    public void prePersist (RoomParticipant player) {
        logger.info("pre persist player");
    }

    @PostPersist
    public void postPersist (RoomParticipant player) {
        logger.info("post persist player");

    }
}
