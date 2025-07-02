package com.example.quizgame.service;

import com.example.quizgame.dto.ChangePasswordRequest;
import com.example.quizgame.dto.RegisterRequest;
import com.example.quizgame.dto.VerifyCodeRequest;
import com.example.quizgame.entity.User;
import com.example.quizgame.entity.VerificationCode;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.reponsitory.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepo;
    @Autowired private VerificationCodeRepository codeRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired
    private JavaMailSender mailSender;
    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public ResponseEntity<?> register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername()))
            return ResponseEntity.badRequest().body("Username đã tồn tại");
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        userRepo.save(user);
        return ResponseEntity.ok("Đăng ký thành công");
    }

    public ResponseEntity<?> changePassword(ChangePasswordRequest req) {
        User user = userRepo.findByUsername(req.getUsername()).orElseThrow();
        if (!encoder.matches(req.getOldPassword(), user.getPassword()))
            return ResponseEntity.badRequest().body("Mật khẩu cũ sai");
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(user);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
    @Transactional
    public ResponseEntity<?> sendVerificationCode(String username) {
        Optional<User> userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Username không tồn tại");
        }

        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        VerificationCode vc = new VerificationCode();
        vc.setUsername(username);
        vc.setCode(code);
        vc.setCreatedAt(LocalDateTime.now());

        codeRepo.deleteByUsername(username);
        codeRepo.save(vc);

        String email = userOpt.get().getEmail();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác minh đổi mật khẩu");
            message.setText("Mã xác minh của bạn là: " + code + "\nMã có hiệu lực trong 10 phút.");
            mailSender.send(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không gửi được email: " + e.getMessage());
        }

        return ResponseEntity.ok("Mã xác minh đã được gửi tới email của bạn.");
    }

    @Transactional
    public ResponseEntity<?> verifyCodeAndChangePassword(VerifyCodeRequest req) {
        Optional<VerificationCode> codeOpt = codeRepo.findByUsernameAndCode(req.getUsername(), req.getCode());
        if (codeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Mã xác minh không đúng");
        }

        VerificationCode vc = codeOpt.get();
        if (vc.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
            return ResponseEntity.badRequest().body("Mã đã hết hạn");
        }

        Optional<User> userOpt = userRepo.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Người dùng không tồn tại");
        }

        User user = userOpt.get();
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(user);
        codeRepo.delete(vc);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority(user.getRole())));
    }
}