package com.example.quizgame.reponsitory;

import com.example.quizgame.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findByRole(User.Role role, Pageable pageable);
    // Nhóm số lượng user theo ngày
    @Query("SELECT DATE(u.createdAt), COUNT(u) " +
            "FROM User u " +
            "WHERE u.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(u.createdAt) " +
            "ORDER BY DATE(u.createdAt)")
    List<Object[]> countUsersByDay(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT u FROM User u WHERE u.role = :role " +
            "AND (:firstname IS NULL OR LOWER(u.firstname) LIKE LOWER(CONCAT('%', :firstname, '%')))")
    Page<User> searchByRoleAndFirstnameLike(@Param("role") User.Role role,
                                            @Param("firstname") String firstname,
                                            Pageable pageable);
}