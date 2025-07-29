package com.example.quizgame.service;

import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public void sendWarningEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        String subject = "Cảnh báo vi phạm nội dung";
        String body = "Xin chào " + user.getFirstname() + ",\n\n"
                + "Bạn đã vi phạm quá nhiều lần trong cuộc trò chuyện.\n"
                + "Hãy chú ý lời nói để tránh bị khóa tài khoản.\n\n"
                + "Trân trọng.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("toanvuvan1405@gmail.com");

        mailSender.send(message);
    }
}