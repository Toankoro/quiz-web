package com.example.quizgame.dto.quiz;

import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.dto.user.UserDTO;
import com.example.quizgame.dto.user.UserProfileResponse;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.entity.User;
import com.example.quizgame.service.UserService;
import lombok.*;
import org.hibernate.internal.build.AllowNonPortable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class QuizResponse {

    private Long id;
    private String topic;
    private String name;
    private String description;
    private boolean visibleTo;
    private String imageUrl; // Thêm trường ảnh bìa
    private List<QuestionResponse> questions;
    private Boolean favorite;
    private UserProfileResponse createdBy;
    private LocalDateTime createdAt;
    private long countTimePlay;

}
