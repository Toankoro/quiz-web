package com.example.quizgame.service;

import com.example.quizgame.entity.SecureToken;
import org.springframework.stereotype.Service;

public interface SecureTokenService {

    SecureToken createToken();

    void saveSecureToken(SecureToken secureToken);

    SecureToken findByToken(String token);

    void removeToken(SecureToken token);
}
