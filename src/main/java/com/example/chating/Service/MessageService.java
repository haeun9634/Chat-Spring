package com.example.chating.Service;

import com.example.chating.domain.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 메시지 저장
    public void saveMessage(Long chatRoomId, Long senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(chatRoomId.toString());
        message.setSender(senderId);
        message.setContent(content);
        message.setSendAt(LocalDateTime.now());

        String key = "chatroom:" + chatRoomId + ":messages";
        redisTemplate.opsForZSet().add(key, message, System.currentTimeMillis());
    }

    // 메시지 조회
    public List<ChatMessage> getMessagesByChatRoom(Long chatRoomId) {
        String key = "chatroom:" + chatRoomId + ":messages";
        Set<Object> messages = redisTemplate.opsForZSet().range(key, 0, -1);
        return messages.stream()
                .map(obj -> (ChatMessage) obj)
                .collect(Collectors.toList());
    }

    // 메시지 읽음 처리
    public void markMessageAsRead(Long messageId, Long userId) {
        // Redis에서는 메시지 읽음 처리를 위한 별도의 키를 관리
        String key = "message:" + messageId + ":readBy";
        redisTemplate.opsForSet().add(key, userId);
    }
}
