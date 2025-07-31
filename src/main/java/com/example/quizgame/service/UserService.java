package com.example.quizgame.service;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.request.ChangePasswordRequest;
import com.example.quizgame.dto.request.LoginRequest;
import com.example.quizgame.dto.request.RegisterRequest;
import com.example.quizgame.dto.request.VerifyCodeRequest;
import com.example.quizgame.dto.response.LoginResponse;
import com.example.quizgame.dto.response.UserResponse;
import com.example.quizgame.dto.user.UserProfileResponse;
import com.example.quizgame.dto.user.UserProfileUpdateRequest;
import com.example.quizgame.entity.SecureToken;
import com.example.quizgame.entity.User;
import com.example.quizgame.entity.VerificationCode;
import com.example.quizgame.exceptions.InvalidTokenException;
import com.example.quizgame.exceptions.UserAlreadyExistException;
import com.example.quizgame.mailing.AccountVerificationEmailContext;
import com.example.quizgame.mailing.EmailService;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.reponsitory.VerificationCodeRepository;
import com.example.quizgame.security.JwtUtil;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired private VerificationCodeRepository codeRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private SecureTokenService secureTokenService;
    @Autowired
    private EmailService emailService;

    @Value("${site.base.url.https}")
    private String baseUrl;

    public ApiResponse<UserResponse> register(RegisterRequest registerRequest) throws UserAlreadyExistException {

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername()))
            throw new UserAlreadyExistException("Người dùng đã tồn tại");
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        userRepository.save(user);
        user.setFirstname("User" + user.getId());
        userRepository.save(user);
        sendRegistrationConfirmationEmail(user);
        ApiResponse<UserResponse> responseRegister = new ApiResponse();
        responseRegister.setData(UserResponse.fromUserToUserResponse(user));
        return responseRegister;
    }


    public void sendRegistrationConfirmationEmail(User user) {
        SecureToken secureToken = secureTokenService.createToken();
        secureToken.setUser(user);
        secureTokenService.saveSecureToken(secureToken);

        AccountVerificationEmailContext context = new AccountVerificationEmailContext();
        context.init(user);
        context.setToken(secureToken.getToken());
        context.buildVerificationUrl(baseUrl, secureToken.getToken());

        try {
            emailService.sendMail(context);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyUser(String token) throws InvalidTokenException {
        SecureToken secureToken = secureTokenService.findByToken(token);
        if (Objects.isNull(token) || !StringUtils.equals(token, secureToken.getToken()) || secureToken.isExpired()) {
            if (secureToken.getUser() != null) {
                userRepository.deleteById(secureToken.getUser().getId());
            }

            secureTokenService.removeToken(secureToken);
            throw new InvalidTokenException("Token đã hết hạn, tài khoản đã bị xóa!");
        }

        User user = userRepository.getById(secureToken.getUser().getId());
        if (Objects.isNull(user)) {
            return false;
        }

        user.setAccountVerified(true);
        userRepository.save(user);

        secureTokenService.removeToken(secureToken);
        return true;
    }

    public ResponseEntity<?> changePassword(ChangePasswordRequest req) {
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        if (!encoder.matches(req.getOldPassword(), user.getPassword()))
            return ResponseEntity.badRequest().body("Mật khẩu cũ sai");
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
    @Transactional
    public ResponseEntity<?> sendVerificationCode(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
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
        // 1. Tìm mã đúng theo username và code
        Optional<VerificationCode> codeOpt = codeRepo.findByUsernameAndCode(req.getUsername(), req.getCode());
        if (codeOpt.isEmpty()) {
            // Nếu mã không khớp, tìm mã gần nhất của user để xử lý attempt
            Optional<VerificationCode> wrongCodeOpt = codeRepo.findTopByUsernameOrderByCreatedAtDesc(req.getUsername());

            if (wrongCodeOpt.isPresent()) {
                VerificationCode wrongCode = wrongCodeOpt.get();
                // Check hết hạn (sau 10 phút)
                if (wrongCode.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                    codeRepo.delete(wrongCode);
                    return ResponseEntity.badRequest().body("Mã đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
                }
                // Tăng số lần thử
                int currentAttempts = wrongCode.getAttempts() + 1;
                if (currentAttempts >= 5) {
                    codeRepo.delete(wrongCode); // Xoá mã sau khi sai quá số lần
                    return ResponseEntity.badRequest().body("Bạn đã nhập sai quá 5 lần. Mã xác minh đã bị huỷ.");
                }
                wrongCode.setAttempts(currentAttempts);
                codeRepo.save(wrongCode);
                return ResponseEntity.badRequest().body("Mã xác minh không đúng. Bạn còn " + (5 - currentAttempts) + " lần thử.");
            }

            // Nếu không có mã nào gần nhất (trường hợp bất thường)
            return ResponseEntity.badRequest().body("Mã xác minh không đúng.");
        }
        // 2. Nếu tìm thấy mã đúng
        VerificationCode vc = codeOpt.get();
        // Kiểm tra hết hạn
        if (vc.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
            codeRepo.delete(vc);
            return ResponseEntity.badRequest().body("Mã đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }
        // Kiểm tra số lần sai
        if (vc.getAttempts() >= 5) {
            codeRepo.delete(vc);
            return ResponseEntity.badRequest().body("Mã đã bị huỷ do vượt quá số lần thử.");
        }
        // 3. Đổi mật khẩu
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Người dùng không tồn tại.");
        }
        User user = userOpt.get();
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepository.save(user);
        // 4. Xoá mã sau khi dùng xong
        codeRepo.delete(vc);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công.");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));
        return new CustomUserDetails(user);
    }

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserProfileResponse.builder()
                .firstname(user.getFirstname())
                .email(user.getEmail())
                .socialLinks(user.getSocialLinks())
                .avatar(user.getAvatar())
                .build();
    }

    public void updateProfile(String username, UserProfileUpdateRequest req) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setFirstname(req.getFirstname());
        user.setEmail(req.getEmail());
        user.setSocialLinks(req.getSocialLinks());

        if (req.getAvatar() != null && !req.getAvatar().isEmpty()) {
            System.out.println("Avatar name: " + req.getAvatar().getOriginalFilename());
            System.out.println("Content type: " + req.getAvatar().getContentType());
            System.out.println("Size: " + req.getAvatar().getSize());
            String contentType = req.getAvatar().getContentType(); // e.g. image/png
            if (contentType != null && contentType.startsWith("image/")) {
                String base64 = Base64.getEncoder().encodeToString(req.getAvatar().getBytes());
                user.setAvatar("data:" + contentType + ";base64," + base64);
            } else {
                throw new IllegalArgumentException("Uploaded file is not an image");
            }
        }

        userRepository.save(user);
    }

    public void increaseExp(User user, int addedExp) {
        int newExp = user.getExp() + addedExp;
        int levelUps = newExp / 100;
        user.setLevel(user.getLevel() + levelUps);
        user.setExp(newExp % 100);
    }

}