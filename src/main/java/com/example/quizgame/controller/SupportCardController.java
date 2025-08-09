package com.example.quizgame.controller;

import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.question.ClientSessionRequest;
import com.example.quizgame.dto.supportcard.SupportCardMessage;
import com.example.quizgame.dto.supportcard.SupportCardType;
import com.example.quizgame.entity.Room;
import com.example.quizgame.reponsitory.RoomRepository;
import com.example.quizgame.service.RoomService;
import com.example.quizgame.service.redis.QuestionRedisService;
import com.example.quizgame.service.SupportCardService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class SupportCardController {
    private final QuestionRedisService questionRedisService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SupportCardService supportCardService;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    // room participant use support card
    @PostMapping("/{pinCode}/support-card")
    public ResponseEntity<?> useSupportCard(
            @PathVariable String pinCode,
            @RequestBody SupportCardMessage supportCardMessage) throws BadRequestException {

        return ResponseEntity.ok(
                supportCardService.useSupportCard(pinCode, supportCardMessage)
        );
    }

    // random card before start game, max random limited is 3
    @PostMapping("/{pinCode}/support-card/random")
    public ResponseEntity<?> randomSupportCards(
            @PathVariable String pinCode,
            @RequestBody ClientSessionRequest clientSessionRequest) {

        String clientSessionId = clientSessionRequest.getClientSessionId();
        String roomLockKey = "card:locked:" + pinCode;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(roomLockKey))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Phòng đã bắt đầu, không thể random thẻ.");
        }
        try {
            List<SupportCardType> cards = questionRedisService.randomTwoCards(pinCode, clientSessionId);
            return ResponseEntity.ok(cards);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // reset room for test
    @PostMapping("/{pinCode}/reset-room")
    public ResponseEntity<?> resetGameRoom(
            @PathVariable String pinCode,
            @RequestParam Long quizId,
            @RequestParam String clientSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Room room = roomRepository.findByPinCode(pinCode).orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng tương ứng với pinCode"));
        if (!roomService.isHostRoom(room.getId(), userDetails.getUser())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền replay game trong phòng này.");
        }
        supportCardService.resetRoomState(pinCode, quizId, clientSessionId);
        return ResponseEntity.ok("Trạng thái phòng đã được reset.");
    }


}
