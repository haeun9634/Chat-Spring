package com.example.chating.Redis;

import com.example.chating.Dto.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            System.out.println("Received message from Redis: " + new String(message.getBody()));
            ChatMessage chatMessage = objectMapper.readValue(message.getBody(), ChatMessage.class);
            System.out.println("Sending to STOMP Broker: " + chatMessage);

            messagingTemplate.convertAndSend("/topic/" + chatMessage.getRoomId(), chatMessage);
        } catch (IOException e) {
            log.error("Failed to process Redis message: ", e);
        }
    }



}
