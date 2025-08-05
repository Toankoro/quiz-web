package com.example.quizgame.service;

import com.example.quizgame.reponsitory.PlayerAnswerRepository;
import com.example.quizgame.reponsitory.QuestionRepository;
import com.example.quizgame.reponsitory.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl {

    private final RoomRepository roomRepository;
    private final QuestionRepository questionRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final RoomParticipantRedisService redisService;


}
