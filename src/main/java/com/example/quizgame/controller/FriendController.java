package com.example.quizgame.controller;

import com.example.quizgame.entity.FriendRequest;
import com.example.quizgame.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/friend")
public class FriendController {

    @Autowired
    private FriendService service;

    @PostMapping("/send")
    public ResponseEntity<?> sendRequest(@RequestParam String sender, @RequestParam String receiver) {
        return ResponseEntity.ok(service.sendRequest(sender, receiver));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptRequest(@RequestParam Long id) {
        return ResponseEntity.ok(service.acceptRequest(id));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteFriend(@RequestParam String user1, @RequestParam String user2) {
        return ResponseEntity.ok(service.deleteFriend(user1, user2));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String keyword, @RequestParam String user) {
        return ResponseEntity.ok(service.searchUsers(keyword, user));
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFriends(@RequestParam String user) {
        return ResponseEntity.ok(service.getFriends(user));
    }
    @PostMapping("/reject")
    public ResponseEntity<?> reject(@RequestParam Long id) {
        return ResponseEntity.ok(service.rejectRequest(id));
    }
    @GetMapping("/received")
    public ResponseEntity<?> getReceivedFriendRequests(@RequestParam String username) {
        List<FriendRequest> requests = service.getReceivedRequests(username);
        List<Map<String, Object>> response = requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("sender", req.getSender().getUsername());
            map.put("createdAt", req.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}