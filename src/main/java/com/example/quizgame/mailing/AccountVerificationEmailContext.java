package com.example.quizgame.mailing;

import com.example.quizgame.entity.User;
import org.springframework.web.util.UriComponentsBuilder;

public class AccountVerificationEmailContext extends AbstractEmailContext{
    private String token;

    @Override
    public <T> void init(T context) {
        User user = (User) context;
        put("firstName", user.getFirstname());
        setTemplateLocation("mailing/email-verification");
        setSubject("Xác nhận đăng ký");
        setFrom("toanvuvan1405@gmail.com");
        setTo(user.getEmail());
    }

    public void setToken(String token) {
        this.token = token;
        put("token", token);
    }

    public void buildVerificationUrl(final String baseURL, final String token) {
        final String url = UriComponentsBuilder.fromHttpUrl(baseURL)
                .path("/register/verify").queryParam("token", token).toUriString();
        put("verificationURL", url);
    }

}
