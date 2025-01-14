package com.example.chating.Service;

import com.example.chating.Repository.MessageRepository;
import com.example.chating.Repository.UserRepository;
import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.chating.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageRepository messageRepository;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    // 메시지 저장
    public void saveMessage(Long chatRoomId, Long senderId, String content) {

        if (chatRoomId == null || senderId == null) {
            throw new IllegalArgumentException("Chat Room ID or Sender ID must not be null.");
        }
        String senderName = userService.getUserNameById(senderId);

        // Redis에 저장
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoomId(chatRoomId.toString());
        chatMessage.setSender(senderId);
        chatMessage.setSenderName(senderName);
        chatMessage.setContent(content);
        chatMessage.setSendAt(LocalDateTime.now());

        String redisKey = "chatroom:" + chatRoomId + ":messages";
        redisTemplate.opsForZSet().add(redisKey, chatMessage, System.currentTimeMillis());

        // 데이터베이스에 저장
        Message messageEntity = Message.builder()
                .chatRoom(chatRoomService.getChatRoomById(chatRoomId))
                .sender(userService.getUserById(senderId))
                .content(content)
                .sentAt(chatMessage.getSendAt())
                .build();

        messageRepository.save(messageEntity); // JPA 저장
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

    @Autowired
    private UserRepository userRepository;

    public List<ChatMessage> getMessagesByChatRoom(Long chatRoomId, int page, int size) {
        String redisKey = "chatroom:" + chatRoomId + ":messages";
        Set<Object> redisMessages = redisTemplate.opsForZSet().range(redisKey, 0, -1);

        // Redis에 데이터가 있는 경우
        if (redisMessages != null && !redisMessages.isEmpty()) {
            return redisMessages.stream()
                    .map(obj -> (ChatMessage) obj)
                    .collect(Collectors.toList());
        }

        // Redis에 데이터가 없는 경우 -> 데이터베이스에서 페이징 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> dbMessages = messageRepository.findByChatRoomId(chatRoomId, pageable);

        return dbMessages.stream()
                .map(message -> {
                    return new ChatMessage(
                            ChatMessage.MessageType.TALK,
                            message.getChatRoom().getId().toString(),
                            message.getSender().getId(),
                            message.getSender().getName(), // 추가된 사용자 이름
                            message.getContent(),
                            message.getSentAt()
                    );
                })
                .collect(Collectors.toList());
    }



    // 메시지 읽음 처리
    public void markMessageAsRead(Long messageId, Long userId) {
        // Redis에서는 메시지 읽음 처리를 위한 별도의 키를 관리
        String key = "message:" + messageId + ":readBy";
        redisTemplate.opsForSet().add(key, userId);
    }

}
