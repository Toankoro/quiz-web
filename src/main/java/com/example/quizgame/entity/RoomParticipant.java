package com.example.quizgame.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne private User user;

    private boolean isHost;

    @OneToMany(mappedBy = "roomParticipant", cascade = CascadeType.ALL)
    private List<PlayerAnswer> answers = new ArrayList<>();

    private String clientSessionId;

}
