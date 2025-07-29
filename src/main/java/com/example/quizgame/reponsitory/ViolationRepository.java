package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.User;
import com.example.quizgame.entity.Violation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    Optional<Violation> findByUser(User user);
    Optional<Violation> findByUserId(Long userId);
    Page<Violation> findAll(Pageable pageable);
}
