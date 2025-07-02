package com.example.quizgame.service;

import com.example.quizgame.entity.ChatMessage;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.ChatMessageRepository;
import com.example.quizgame.reponsitory.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private SimpMessagingTemplate template;

    public List<ChatMessage> getMessages(String group) {
        return chatRepo.findByGroupNameOrderByTimestampAsc(group);
    }

    public ChatMessage send(String group, String content, MultipartFile file, String type, Principal principal) throws IOException {
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        ChatMessage msg = new ChatMessage();
        msg.setSender(user);
        msg.setGroupName(group);
        msg.setTimestamp(LocalDateTime.now());
        msg.setType(type);

        if ("TEXT".equals(type) || "EMOJI".equals(type)) {
            msg.setContent(content);
        } else if ("FILE".equals(type) && file != null) {
            msg.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
            msg.setFileName(file.getOriginalFilename());
            msg.setFileType(file.getContentType());
        }

        chatRepo.save(msg);
        template.convertAndSend("/topic/" + group, msg);
        return msg;
    }

    public ResponseEntity<byte[]> downloadFile(Long id) {
        ChatMessage msg = chatRepo.findById(id).orElseThrow();
        if (!"FILE".equals(msg.getType())) return ResponseEntity.badRequest().build();

        byte[] data = Base64.getDecoder().decode(msg.getContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + msg.getFileName())
                .contentType(MediaType.parseMediaType(msg.getFileType()))
                .body(data);
    }
}