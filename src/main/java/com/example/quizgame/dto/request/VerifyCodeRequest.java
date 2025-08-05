package com.example.quizgame.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCodeRequest {
    private String username;
    private String code;
    private String newPassword;
}
