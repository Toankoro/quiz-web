package com.example.quizgame.entity;

import com.example.quizgame.dto.PlayerAnswer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Player implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // sessionid websocket
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private GameRoom room;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<PlayerAnswer> answers = new ArrayList<>();

    // session client id reconnect
    @Column(name = "client_session_id")
    private String clientSessionId;
}
