package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByUsernameAndCode(String username, String code);
    void deleteByUsername(String username);
    Optional<VerificationCode> findTopByUsernameOrderByCreatedAtDesc (String username);
}