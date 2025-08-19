package com.example.quizgame.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
public class RoomParticipant implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    private User user;

    private boolean isHost;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String avatar;

    private String clientSessionId;

}
