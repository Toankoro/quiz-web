package com.example.quizgame.controller;

import com.example.quizgame.dto.question.ReconnectResponse;
import com.example.quizgame.service.ReconnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ReconnectController {

    private final ReconnectService reconnectService;

    @GetMapping("/{pinCode}/reconnect")
    public ResponseEntity<ReconnectResponse> reconnect(
            @PathVariable String pinCode,
            @RequestParam String clientSessionId
    ) {
        ReconnectResponse response = reconnectService.reconnect(pinCode, clientSessionId);
        return ResponseEntity.ok(response);
    }
}
