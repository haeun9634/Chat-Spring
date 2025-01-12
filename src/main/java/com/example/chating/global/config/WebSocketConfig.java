package com.example.chating.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 메시지 브로커
        config.setApplicationDestinationPrefixes("/app"); // 클라이언트 요청 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // WebSocket 연결 URL
                .setAllowedOrigins("http://localhost:3000")  // 프론트엔드의 도메인을 지정해야 할 수도 있음
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthenticationInterceptor());
    }

    public class StompAuthenticationInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = accessor.getFirstNativeHeader("Authorization");
                // JWT 인증 로직
                if (isValidToken(token)) {
                    String username = extractUsernameFromToken(token);
                    Long userId = extractUserIdFromToken(token);
                    accessor.setUser(new AuthenticatedUser(username, userId));
                } else {
                    throw new IllegalArgumentException("Invalid Token");
                }
            }
            return message;
        }

        private boolean isValidToken(String token) {
            // JWT 검증 로직 (예: JWT 라이브러리 사용)
            return token != null && token.startsWith("Bearer ");
        }

        private String extractUsernameFromToken(String token) {
            // JWT에서 사용자명 추출
            return "exampleUser";
        }

        private Long extractUserIdFromToken(String token) {
            // JWT에서 사용자 ID 추출
            return 1L;
        }
    }

}
