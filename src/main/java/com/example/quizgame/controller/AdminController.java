package com.example.quizgame.controller;

import com.example.quizgame.dto.user.UserDTO;
import com.example.quizgame.dto.user.ViolationDTO;
import com.example.quizgame.entity.User;
import com.example.quizgame.entity.Violation;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.reponsitory.ViolationRepository;
import com.example.quizgame.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ViolationRepository violationRepo;

    @Autowired
    private EmailService emailService;

    // Danh sách user (chỉ USER), phân trang
    @GetMapping("/users")
    public Page<UserDTO> getUsers(@RequestParam int page, @RequestParam int size) {
        Page<User> users = userRepo.findByRole(User.Role.USER, PageRequest.of(page, size));
        return users.map(user -> new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getEmail()
        ));
    }

    // Khóa tài khoản
    @PostMapping("/ban/{id}")
    public void banUser(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow();
        user.setLoginDisabled(false);
        userRepo.save(user);
    }

    // Mở khóa
    @PostMapping("/unban/{id}")
    public void unbanUser(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow();
        user.setLoginDisabled(true);
        userRepo.save(user);
    }

    // Xóa tài khoản
    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow();
        userRepo.delete(user);
        return ResponseEntity.ok("Đã xóa user và dữ liệu liên quan");
    }
    // Danh sách vi phạm (có phân trang)
    @GetMapping("/violations")
    public Page<ViolationDTO> getViolations(@RequestParam int page, @RequestParam int size) {
        Page<Violation> violations = violationRepo.findAll(PageRequest.of(page, size));
        return violations.map(v -> new ViolationDTO(
                v.getId(),
                v.getUser().getUsername(),
                v.getUser().getEmail(),
                v.getTimestamp().toLocalDate(), // chỉ lấy ngày
                v.getLevel()
        ));
    }

    @PostMapping("/warning/{userId}")
    public ResponseEntity<?> sendWarning(@PathVariable Long userId) {
        emailService.sendWarningEmail(userId);
        return ResponseEntity.ok("Đã gửi cảnh báo tới user " + userId);
    }
}
