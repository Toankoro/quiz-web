package com.example.quizgame.controller;

import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.room.ParticipantDTO;
import com.example.quizgame.dto.user.UserProfileUpdateRequest;
import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.RoomParticipant;
import com.example.quizgame.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestParam Long quizId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(roomService.createRoom(quizId, userDetails.getUser()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestParam String pin,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(roomService.joinRoom(pin, userDetails.getUser()));
    }

    @PostMapping("/leave/{roomId}")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        roomService.leaveRoom(userDetails.getUser(), roomId);
        return ResponseEntity.ok("Đã thoát phòng");
    }

    @DeleteMapping("/kick/{roomId}/{userId}")
    public ResponseEntity<?> kickUser(@PathVariable Long roomId,
                                      @PathVariable Long userId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        roomService.kickUser(userDetails.getUser(), roomId, userId);
        return ResponseEntity.ok("Đã xóa người chơi");
    }

    @GetMapping("/participants")
    public ResponseEntity<?> listParticipants(@RequestParam Long roomId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(roomService.getParticipantsByRoom(roomId, userDetails.getUser()));
    }


    @GetMapping("/{roomId}/qrcode")
    public ResponseEntity<Map<String, String>> getRoomQRCode(@PathVariable Long roomId) {
        Room room = roomService.getRoomById(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("pinCode", room.getPinCode());
        response.put("qrCodeUrl", room.getQrCodeUrl());
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long roomId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        roomService.deleteRoomByHost(roomId, userDetails.getUser());
        return ResponseEntity.ok("Đã xóa phòng");
    }

    @PostMapping("/start/{roomId}")
    public ResponseEntity<?> startRoom(@PathVariable Long roomId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ParticipantDTO> participants = roomService.startRoom(roomId, userDetails.getUser());
        return ResponseEntity.ok(participants);
    }
    @PutMapping("/{roomId}/avatar")
    public ResponseEntity<ParticipantDTO> updateAvatar(
            @PathVariable Long roomId,
            @ModelAttribute UserProfileUpdateRequest req,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ParticipantDTO updated = roomService.updateAvatarRoom(roomId, userId, req);
        return ResponseEntity.ok(updated);
    }


}
