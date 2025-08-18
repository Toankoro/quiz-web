package com.example.quizgame.websocket;

import com.example.quizgame.service.redis.RoomParticipantRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

    private final RoomParticipantRedisService roomParticipantRedisService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String clientSessionId = accessor.getFirstNativeHeader("clientSessionId");
            String pinCode = accessor.getFirstNativeHeader("pinCode");
            log.info("CONNECT header clientSessionId={}, pinCode={}", clientSessionId, pinCode);

            try {
                boolean valid = roomParticipantRedisService.isValidClientSession(pinCode, clientSessionId);
                log.info("Check session result: {}", valid);

                if (clientSessionId != null && pinCode != null && valid) {
                    log.info("Đang gán Principal {}", clientSessionId);
                    accessor.setUser(new StompPrincipal(clientSessionId));
                } else {
                    log.warn("SessionId hoặc PinCode không hợp lệ -> reject");
                    throw new IllegalArgumentException("ClientSessionId hoặc PinCode không hợp lệ");
                }
            } catch (Exception e) {
                log.error("Lỗi khi check session trong Redis", e);
                throw e;
            }
        }


        return message;
    }
}

