package com.example.quizgame.service;

import com.example.quizgame.entity.SecureToken;

public interface SecureTokenService {

    SecureToken createToken();

    void saveSecureToken(SecureToken secureToken);

    SecureToken findByToken(String token);

    void removeToken(SecureToken token);
}
