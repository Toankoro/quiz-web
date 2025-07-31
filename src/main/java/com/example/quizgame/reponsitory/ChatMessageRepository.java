package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByGroupNameOrderByTimestampAsc(String groupName);
}
