package com.example.quizgame.service;

import com.example.quizgame.dto.chat.ChatMessageDTO;
import com.example.quizgame.entity.ChatMessage;
import com.example.quizgame.entity.User;
import com.example.quizgame.entity.Violation;
import com.example.quizgame.reponsitory.ChatMessageRepository;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.reponsitory.ViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ChatConsumer {

    @Autowired
    private ChatMessageRepository chatRepo;

    @Autowired
    private BadWordFilterService badWordFilterService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ViolationRepository violationRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "chat-topic", groupId = "chat-group", containerFactory = "chatKafkaListenerContainerFactory")
    public void consume(ChatMessageDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setGroupName(dto.getGroupName());
        // Lọc từ bậy
        String filteredContent = badWordFilterService.filter(dto.getContent());
        message.setContent(filteredContent);

        //  Tìm User từ senderId
        User sender = userRepo.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + dto.getSenderId()));

        message.setSender(sender);
        message.setTimestamp(LocalDateTime.now());
        chatRepo.save(message);

        if (badWordFilterService.containsBadWords(dto.getContent())) {
            LocalDate today = LocalDate.now();

            // Tìm vi phạm trong ngày hôm nay của user
            Optional<Violation> optionalViolation = violationRepo.findByUserAndDate(sender, today);

            Violation violation;
            if (optionalViolation.isPresent()) {
                // Đã có vi phạm hôm nay → cập nhật số lần
                violation = optionalViolation.get();
                violation.setCount(violation.getCount() + 1);
            } else {
                // Chưa có bản ghi hôm nay → tạo mới
                violation = new Violation();
                violation.setUser(sender);
                violation.setDate(today);
                violation.setCount(1);
            }

            // Xác định mức độ vi phạm
            int count = violation.getCount();
            if (count <= 3) {
                violation.setLevel("Nhẹ");
            } else if (count <= 7) {
                violation.setLevel("Trung bình");
            } else {
                violation.setLevel("Nặng");
            }

            // Lưu lại
            violationRepo.save(violation);
        }

    }
}
