package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.User;
import com.example.quizgame.entity.Violation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    Optional<Violation> findByUser(User user);
    Optional<Violation> findByUserAndDate(User user, LocalDate date);
    Page<Violation> findAll(Pageable pageable);
}
