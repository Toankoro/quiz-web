package com.example.quizgame.controller;

import com.example.quizgame.dto.chat.ChatMessageDTO;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.entity.ChatMessage;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.ChatMessageRepository;
import com.example.quizgame.service.BadWordFilterService;
import com.example.quizgame.service.ChatProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatProducer chatProducer;
    @Autowired
    private ChatMessageRepository chatRepo;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private BadWordFilterService badWordFilterService;
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody ChatMessageDTO dto,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        dto.setSenderId(user.getId());
        dto.setSenderUsername(user.getUsername());
        chatProducer.sendMessage(dto);
        String filteredContent = badWordFilterService.filter(dto.getContent());
        dto.setContent(filteredContent);
        messagingTemplate.convertAndSend("/topic/chat/" + dto.getGroupName(), dto);
        return ResponseEntity.ok("Đã gửi");
    }


    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable String groupName) {
        List<ChatMessage> list = chatRepo.findByGroupNameOrderByTimestampAsc(groupName);
        List<ChatMessageDTO> result = list.stream().map(ChatMessageDTO::fromEntity).toList();
        return ResponseEntity.ok(result);
    }
}
