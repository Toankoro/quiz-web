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

        // Lọc từ bậy
        String filteredContent = badWordFilterService.filter(dto.getContent());
        message.setContent(filteredContent);

        //  Tìm User từ senderId
        User sender = userRepo.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + dto.getSenderId()));

        message.setSender(sender);
        message.setTimestamp(LocalDateTime.now());

        chatRepo.save(message);
        System.out.println(" Đã lưu tin nhắn đã lọc: " + filteredContent);

        if (badWordFilterService.containsBadWords(dto.getContent())) {
            LocalDate today = LocalDate.now();

            Violation violation = violationRepo.findByUser(sender).orElseGet(() -> {
                Violation v = new Violation();
                v.setUser(sender);
                v.setCount(0);
                v.setTimestamp(LocalDateTime.now());
                return v;
            });
            // Kiểm tra nếu ngày khác thì reset count
            LocalDate lastViolatedDate = violation.getTimestamp().toLocalDate();
            if (!today.equals(lastViolatedDate)) {
                violation.setCount(1); // reset count về 1
            } else {
                violation.setCount(violation.getCount() + 1);
            }
            // Cập nhật thời gian
            violation.setTimestamp(LocalDateTime.now());
            int count = violation.getCount();
            if (count <= 3) {
                violation.setLevel("Nhẹ");
            } else if (count <= 7) {
                violation.setLevel("Trung bình");
            } else {
                violation.setLevel("Nặng");
            }
            violationRepo.save(violation);
        }
    }
}
