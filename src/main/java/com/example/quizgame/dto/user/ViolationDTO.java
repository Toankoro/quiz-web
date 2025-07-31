package com.example.quizgame.dto.user;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDTO {
    private Long id;
    private String username;
    private String email;
    private LocalDate date; // chỉ ngày tháng năm
    private String level;
}
