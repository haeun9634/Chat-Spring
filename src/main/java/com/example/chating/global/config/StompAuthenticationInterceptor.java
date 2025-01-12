package com.example.chating.global.config;

import com.example.chating.global.TokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final TokenProvider tokenProvider;

    public StompAuthenticationInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            // JWT 인증 로직
            if (tokenProvider.isValidToken(token)) {
                String username = tokenProvider.extractUsernameFromToken(token);
                Long userId = tokenProvider.extractUserIdFromToken(token);
                accessor.setUser(new AuthenticatedUser(username, userId));  // 인증된 사용자 설정
            } else {
                throw new IllegalArgumentException("Invalid Token");
            }
        }
        return message;
    }
}
