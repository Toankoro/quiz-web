package com.example.quizgame.dto.chat;

import com.example.quizgame.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String content;
    private String groupName;
    private Long senderId;
    private String senderUsername;
    private LocalDateTime timestamp;

    public static ChatMessageDTO fromEntity(ChatMessage m) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setContent(m.getContent());
        dto.setGroupName(m.getGroupName());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderUsername(m.getSender().getUsername());
        dto.setTimestamp(m.getTimestamp());
        return dto;
    }
}
