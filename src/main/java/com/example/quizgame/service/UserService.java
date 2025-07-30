package com.example.quizgame.service;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.request.ChangePasswordRequest;
import com.example.quizgame.dto.request.RegisterRequest;
import com.example.quizgame.dto.request.VerifyCodeRequest;
import com.example.quizgame.dto.response.UserResponse;
import com.example.quizgame.entity.SecureToken;
import com.example.quizgame.entity.User;
import com.example.quizgame.entity.VerificationCode;
import com.example.quizgame.exceptions.InvalidTokenException;
import com.example.quizgame.exceptions.UserAlreadyExistException;
import com.example.quizgame.mailing.AccountVerificationEmailContext;
import com.example.quizgame.mailing.EmailService;
import com.example.quizgame.reponsitory.UserRepository;
import com.example.quizgame.reponsitory.VerificationCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;

@Service
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

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    @Value("${site.base.url.https}")
    private String baseUrl;

    public ApiResponse<UserResponse> register(RegisterRequest registerRequest) throws UserAlreadyExistException {
//        if (checkIfUserExist(registerRequest.getEmail())) {
//            throw new UserAlreadyExistException("This user already exist");
//        }
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername()))
            throw new UserAlreadyExistException("Người dùng đã tồn tại");
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFirstname(registerRequest.getFirstname());
        user.setLastname(registerRequest.getLastname());
        userRepository.save(user);
        sendRegistrationConfirmationEmail(user);
        ApiResponse<UserResponse> responseRegister = new ApiResponse();
        responseRegister.setData(UserResponse.fromUserToUserResponse(user));
        return responseRegister;
    }

    public boolean checkIfUserExist(String email) {
        return userRepository.findByEmail(email) != null;
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
            throw new InvalidTokenException("Token không hợp lệ!");
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
        Optional<VerificationCode> codeOpt = codeRepo.findByUsernameAndCode(req.getUsername(), req.getCode());
        if (codeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Mã xác minh không đúng");
        }

        VerificationCode vc = codeOpt.get();
        if (vc.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
            return ResponseEntity.badRequest().body("Mã đã hết hạn");
        }

        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Người dùng không tồn tại");
        }

        User user = userOpt.get();
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepository.save(user);
        codeRepo.delete(vc);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority(user.getRole())));
    }
}