package com.example.quizgame.service;

import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.question.QuestionResponseToParticipant;
import com.example.quizgame.dto.room.ParticipantDTO;
import com.example.quizgame.dto.room.RoomJoinResponse;
import com.example.quizgame.dto.room.RoomResponse;
import com.example.quizgame.dto.user.UserProfileUpdateRequest;
import com.example.quizgame.entity.*;
import com.example.quizgame.qr.QRCodeGenerator;
import com.example.quizgame.reponsitory.*;
import com.example.quizgame.security.JwtUtil;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.redis.RoomParticipantRedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepo;
    private final RoomParticipantRepository participantRepo;
    private final QuizRepository quizRepo;
    private final QRCodeGenerator qrCodeGenerator;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final RoomParticipantRedisService roomParticipantRedisService;
    private final QuestionRedisService questionRedisService;
    private final UserRepository userRepository;
    private final GameRankingRepository gameRankingRepo;
    private final RoomParticipantRepository roomParticipantRepository;

    public RoomResponse createRoom(Long quizId, User host) {
        if (participantRepo.existsByUserAndRoomStartedAtIsNull(host)) {
            throw new RuntimeException("Bạn đang ở trong một phòng khác.");
        }

        Quiz quiz = quizRepo.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));
        String pin = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        String qrUrl = qrCodeGenerator.generateBase64QRCode("JOIN-" + pin);

        Room room = new Room();
        room.setPinCode(pin);
        room.setQrCodeUrl(qrUrl);
        room.setHost(host);
        room.setQuiz(quiz);

        // Lưu room trước để có ID
        Room savedRoom = roomRepo.save(room);

        // Tạo và lưu RoomParticipant cho host
        RoomParticipant participant = new RoomParticipant();
        participant.setRoom(savedRoom);
        participant.setUser(host);
        participant.setHost(true);
        
        // Lưu participant vào database
        participantRepo.save(participant);

        return RoomResponse.from(savedRoom);
    }

    public RoomJoinResponse joinRoom(String pin, User user) {
        Room room = roomRepo.findByPinCodeAndStartedAtIsNull(pin)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại hoặc đã bắt đầu"));

        if (participantRepo.existsByUserAndRoomStartedAtIsNull(user)) {
            throw new RuntimeException("Bạn đang ở trong một phòng khác.");
        }
        if (room.getStartedAt() != null) {
            throw new RuntimeException("Phòng đã được bắt đầu. Không thể tham gia.");
        }
        String clientSessionId = roomParticipantRedisService.createAndStoreClientSession(pin, user.getUsername());

        RoomParticipant p = new RoomParticipant();
        p.setRoom(room);
        p.setUser(user);
        p.setHost(false);
        p.setClientSessionId(clientSessionId);
        participantRepo.save(p);
        userService.increaseExp(user, 10); // Cộng 10 EXP mỗi lần tham gia
        userRepository.save(user);

        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), getParticipants(room));
        return RoomJoinResponse.from(room, clientSessionId);
    }

    public List<ParticipantDTO> startRoom(Long roomId, User user) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này"));

        if (!participant.isHost()) {
            throw new RuntimeException("Chỉ chủ phòng mới được bắt đầu trò chơi");
        }

        if (room.getStartedAt() != null) {
            throw new RuntimeException("Phòng đã được bắt đầu");
        }

        room.setStartedAt(LocalDateTime.now());
        roomRepo.save(room);

        Long quizId = room.getQuiz().getId();

        List<QuestionResponse> questions = questionRedisService.getQuestionsByQuizId(quizId);
        questionRedisService.setQuizIdByPinCode(room.getPinCode(), quizId);
        questionRedisService.setCurrentQuestionId(room.getPinCode(), questions.get(0).getId());

        // Tạo dữ liệu GameRanking cho từng người chơi trong phòng
        List<RoomParticipant> participants = room.getParticipants();
        List<GameRanking> rankings = new ArrayList<>();

        for (RoomParticipant p : participants) {
            if (!p.isHost()) {
                GameRanking r = new GameRanking();
                r.setRoom(room);
                r.setUser(p.getUser());
                r.setScore(0);
                r.setRanking(0);
                rankings.add(r);
            }
        }
        questionRedisService.setCurrentQuestionIndex(room.getPinCode(), 0);
        questionRedisService.lockRoomAndCommitCards(room.getPinCode());
        gameRankingRepo.saveAll(rankings);
        
        // Gửi câu hỏi đầu tiên cho tất cả participants (bao gồm cả host và players)
        boolean isQuestionLast = false;
        if (questions.size() <= 1){
            isQuestionLast = true;
        }
        
        QuestionResponseToParticipant firstQuestion = QuestionResponseToParticipant.fromQuestionResponseToQuestionResponseToParticipant(questions.get(0), isQuestionLast);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, firstQuestion);

        // Trả về danh sách người chơi KHÔNG phải host
        return participants.stream()
                .filter(p -> !p.isHost())
                .map(p -> new ParticipantDTO(p.getUser().getId(), p.getUser().getFirstname(), p.getAvatar(),false))
                .toList();
    }

    public boolean isHostRoom (Long roomId, User user) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này"));

        return participant.isHost();
    }

    public void leaveRoom(User user, Long roomId) {
        RoomParticipant p = participantRepo.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này."));

        Room room = roomRepo.findById(roomId).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng tương ứng với roomId"));
        roomParticipantRedisService.removeClientSession(room.getPinCode(), p.getClientSessionId());
        participantRepo.delete(p);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, getParticipants(p.getRoom()));
    }

    public void kickUser(User host, Long roomId, Long targetId) {
        RoomParticipant hostP = participantRepo.findByRoomIdAndUserIdAndRoom_StartedAtIsNull(roomId, host.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng hoặc không phải chủ phòng."));

        if (!hostP.isHost()) throw new RuntimeException("Bạn không phải chủ phòng.");

        RoomParticipant target = participantRepo.findByRoomIdAndUserIdAndRoom_StartedAtIsNull(roomId, targetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng trong phòng."));
        Room room = roomRepo.findById(roomId).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng tương ứng với roomId"));
        
        // Gửi thông báo kick riêng cho user bị kick trước khi xóa session
        String clientSessionId = target.getClientSessionId();
        if (clientSessionId != null) {
            System.out.println("=== KICK USER DEBUG ===");
            System.out.println("Sending kick message to clientSessionId: " + clientSessionId);
            System.out.println("Target user: " + target.getUser().getFirstname());
            
            messagingTemplate.convertAndSendToUser(
                clientSessionId, 
                "/queue/kick", 
                "Bạn đã bị kick khỏi phòng bởi chủ phòng!"
            );
            
            System.out.println("Kick message sent successfully");
        } else {
            System.out.println("WARNING: clientSessionId is null, cannot send kick message");
        }
        
        roomParticipantRedisService.removeClientSession(room.getPinCode(), target.getClientSessionId());
        participantRepo.delete(target);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, getParticipants(hostP.getRoom()));
    }


    public List<ParticipantDTO> getParticipantsByRoom(Long roomId, User user) {
        // Kiểm tra user có ở trong phòng không
        RoomParticipant p = participantRepo.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này."));
        Room room = p.getRoom();
        return getParticipants(room);
    }


    private List<ParticipantDTO> getParticipants(Room room) {
        return room.getParticipants().stream().map(rp ->
                new ParticipantDTO(rp.getUser().getId(), rp.getUser().getFirstname(),rp.getAvatar(), rp.isHost())
        ).toList();
    }

    public Room getRoomById(Long roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + roomId));
    }

    @Transactional
    public void deleteRoomByHost(Long roomId, User user) {
        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này"));

        if (!participant.isHost()) {
            throw new AccessDeniedException("Bạn không phải chủ phòng");
        }
        // Xóa bản ghi liên quan
        gameRankingRepo.deleteByRoomId(roomId);
        participantRepo.deleteAllByRoomId(roomId);
        roomRepo.deleteById(roomId);
    }

    public ParticipantDTO updateAvatarRoom(Long roomId, Long userId, UserProfileUpdateRequest req) {
        RoomParticipant participant = roomParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("RoomParticipant not found in this room"));

        MultipartFile file = req.getAvatar();
        try {
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                participant.setAvatar("data:" + contentType + ";base64," + base64);
            } else {
                throw new IllegalArgumentException("Uploaded file is not an image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error processing file", e);
        }

        RoomParticipant saved = roomParticipantRepository.save(participant);

        return new ParticipantDTO(
                saved.getId(),
                saved.getUser().getFirstname(),
                saved.getAvatar(),
                saved.isHost()
        );
    }

}
