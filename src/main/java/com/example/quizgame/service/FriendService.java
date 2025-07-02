package com.example.quizgame.service;

import com.example.quizgame.entity.FriendRequest;
import com.example.quizgame.entity.User;
import com.example.quizgame.reponsitory.FriendRequestRepository;
import com.example.quizgame.reponsitory.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendRequestRepository repo;
    @Autowired private UserRepository userRepo;
    public String sendRequest(String sender, String receiver) {
        if (sender.equals(receiver)) return "Không thể gửi kết bạn cho chính mình";

        boolean exists = repo.existsBySenderUsernameAndReceiverUsernameAndAcceptedFalse(sender, receiver)
                || repo.existsBySenderUsernameAndReceiverUsernameAndAcceptedFalse(receiver, sender);

        if (exists) return "Đã gửi lời mời trước đó hoặc đang chờ xác nhận";

        FriendRequest fr = new FriendRequest();
        fr.setSender(userRepo.findByUsername(sender).orElseThrow());
        fr.setReceiver(userRepo.findByUsername(receiver).orElseThrow());
        fr.setAccepted(false);
        fr.setCreatedAt(LocalDateTime.now());

        repo.save(fr);
        return "Đã gửi lời mời kết bạn";
    }
    public List<FriendRequest> getReceivedRequests(String username) {
        return repo.findByReceiverUsernameAndAcceptedFalse(username);
    }

    public String rejectRequest(Long id) {
        repo.deleteById(id);
        return "Đã từ chối lời mời kết bạn";
    }

    public String acceptRequest(Long id) {
        FriendRequest fr = repo.findById(id).orElseThrow();
        fr.setAccepted(true);
        repo.save(fr);
        return "Đã chấp nhận";
    }

    public String deleteFriend(String user1, String user2) {
        var list = repo.findBySenderUsernameOrReceiverUsernameAndAcceptedTrue(user1, user2);
        list.stream().filter(fr ->
                (fr.getSender().getUsername().equals(user1) && fr.getReceiver().getUsername().equals(user2)) ||
                        (fr.getSender().getUsername().equals(user2) && fr.getReceiver().getUsername().equals(user1))
        ).forEach(repo::delete);
        return "Đã xóa kết bạn";
    }

    public List<String> searchUsers(String keyword, String exclude) {
        return userRepo.findAll().stream()
                .map(User::getUsername)
                .filter(u -> !u.equals(exclude) && u.contains(keyword))
                .collect(Collectors.toList());
    }

    public List<String> getFriends(String username) {
        return repo.findBySenderUsernameOrReceiverUsernameAndAcceptedTrue(username, username).stream()
                .map(fr -> fr.getSender().getUsername().equals(username) ? fr.getReceiver().getUsername() : fr.getSender().getUsername())
                .collect(Collectors.toList());
    }
}
