package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverUsernameAndAcceptedFalse(String username);
    List<FriendRequest> findBySenderUsernameAndAcceptedFalse(String username);
    Optional<FriendRequest> findBySenderUsernameAndReceiverUsername(String sender, String receiver);
    List<FriendRequest> findBySenderUsernameOrReceiverUsernameAndAcceptedTrue(String u1, String u2);
    boolean existsBySenderUsernameAndReceiverUsernameAndAcceptedFalse(String sender, String receiver);

}
