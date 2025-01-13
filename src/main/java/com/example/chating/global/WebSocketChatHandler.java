package com.example.chating.global;

import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.Redis.RedisPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
        String topic = "chatroom." + chatMessage.getRoomId();
        redisPublisher.publish(topic, chatMessage); // Redis Pub/Sub로 메시지 브로드캐스트
    }
}