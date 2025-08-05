package com.example.quizgame.mailing;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendMail(final AbstractEmailContext email) throws MessagingException;
}
