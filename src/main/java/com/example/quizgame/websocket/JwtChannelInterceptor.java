package com.example.quizgame.websocket;

import com.example.quizgame.security.JwtUtil;
import com.example.quizgame.service.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtChannelInterceptor(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Lấy token từ header
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    String username = jwtUtil.extractUsername(token);
                    UserDetails userDetails = userService.loadUserByUsername(username);

                    if (jwtUtil.isTokenValid(token, userDetails)) {
                        // Tạo Authentication object và set vào accessor
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        accessor.setUser(authentication);

                        // Log để debug
                        System.out.println("WebSocket authenticated user: " + username);
                    } else {
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    System.err.println("WebSocket authentication failed: " + e.getMessage());
                    throw new IllegalArgumentException("Authentication failed");
                }
            } else {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
        }
        return message;
    }
}