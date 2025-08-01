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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @KafkaListener(topics = "chat-topic", groupId = "chat-group")
    public void consume(ChatMessageDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setGroupName(dto.getGroupName());
        //  Tìm User từ senderId
        User sender = userRepo.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + dto.getSenderId()));

        message.setSender(sender);
        message.setTimestamp(LocalDateTime.now());
        chatRepo.save(message);

        if (badWordFilterService.containsBadWords(dto.getContent())) {
            LocalDate today = LocalDate.now();

            Violation violation = violationRepo.findByUser(sender).orElseGet(() -> {
                Violation v = new Violation();
                v.setUser(sender);
                v.setCount(0);
                v.setDate(today);  // Gán ngày hôm nay
                return v;
            });

            if (!today.equals(violation.getDate())) {
                violation.setCount(1); // reset về 1 nếu là ngày mới
            } else {
                violation.setCount(violation.getCount() + 1);
            }

            // Phân loại vi phạm
            int count = violation.getCount();
            if (count <= 3) {
                violation.setLevel("Nhẹ");
            } else if (count <= 7) {
                violation.setLevel("Trung bình");
            } else {
                violation.setLevel("Nặng");
            }

            violation.setDate(today); // Cập nhật lại ngày vi phạm
            violationRepo.save(violation);
        }


    }
}
