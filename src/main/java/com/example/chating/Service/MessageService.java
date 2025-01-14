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

//    // 메시지 조회
//    public List<ChatMessage> getMessagesByChatRoom(Long chatRoomId) {
//        String key = "chatroom:" + chatRoomId + ":messages";
//        Set<Object> messages = redisTemplate.opsForZSet().range(key, 0, -1);
//        return messages.stream()
//                .map(obj -> (ChatMessage) obj)
//                .collect(Collectors.toList());
//    }

    private static final String CHAT_MESSAGES_KEY = "chat:messages";

    // 채팅방 메시지 조회
    public List<ChatMessage> getMessagesByChatRoom(Long chatRoomId) {
        List<Object> messageKeys = redisTemplate.opsForList().range("chatroom:" + chatRoomId + ":messages", 0, -1);

        if (messageKeys == null || messageKeys.isEmpty()) {
            return List.of();  // 메시지가 없으면 빈 리스트 반환
        }

        // 메시지 키를 사용하여 메시지 객체를 가져오기
        return messageKeys.stream()
                .map(key -> (String) key)  // Object를 String으로 변환
                .map(key -> redisTemplate.opsForHash().get(CHAT_MESSAGES_KEY, key))
                .filter(messageObj -> messageObj instanceof ChatMessage)
                .map(messageObj -> (ChatMessage) messageObj)
                .collect(Collectors.toList());
    }

    // 메시지 읽음 처리
    public void markMessageAsRead(Long messageId, Long userId) {
        // Redis에서는 메시지 읽음 처리를 위한 별도의 키를 관리
        String key = "message:" + messageId + ":readBy";
        redisTemplate.opsForSet().add(key, userId);
    }
}
