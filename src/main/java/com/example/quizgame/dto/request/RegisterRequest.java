package com.example.quizgame.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Không được để trống thông tin")
    private String username;

    @Email(message = "Không đúng định dạng email. Vui lòng nhập lại")
    @NotBlank(message = "Không được để trống thông tin")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận lại mật khẩu là bắt buộc")
    private String confirmPassword;
}
