package com.example.chating.Service;

import com.example.chating.Repository.MessageRepository;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.MessageType;
import com.example.chating.domain.chat.Message;
import jakarta.transaction.Transactional;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageRepository messageRepository;
    @Autowired
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    private static final String CHAT_ROOM_MESSAGES_KEY = "chatroom:%s:messages"; // 메시지 리스트
    private static final String CHAT_ROOM_ACTIVITY_KEY = "chatroom:activity"; // 채팅방 활동 시간
    private static final String CHAT_ROOM_LATEST_MESSAGE_KEY = "chatroom:%s:latestMessage"; // 최신 메시지

    // 메시지 저장
    @Transactional
    public ChatMessage saveMessage(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        if (chatRoomId == null || senderId == null) {
            throw new IllegalArgumentException("Chat Room ID or Sender ID must not be null.");
        }
        // 고유한 ID 생성
        String uniqueId = UUID.randomUUID().toString();

        String senderName = userService.getUserNameById(senderId);

        // 메시지 객체 생성
        ChatMessage chatMessage = new ChatMessage(
                uniqueId,
                messageType,
                chatRoomId.toString(),
                senderId,
                senderName,
                content,
                LocalDateTime.now()
        );

        // Redis에 메시지 저장
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);
        redisTemplate.opsForList().rightPush(chatRoomMessagesKey, chatMessage);

        // 최신 메시지 및 활동 시간 업데이트
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        redisTemplate.opsForValue().set(latestMessageKey, content);
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());


        return chatMessage;
    }

//    @Transactional
//    public void updateReadStatus(Long chatRoomId, Long userId) {
//        User user = userService.getUserById(userId);
//
//        // 읽지 않은 메시지 가져오기
//        List<Message> unreadMessages = messageRepository.findUnreadMessagesByChatRoomIdAndUserId(chatRoomId, userId);
//        System.out.println("Found unread messages: " + unreadMessages.size()); // 디버깅 로그
//
//        // 읽지 않은 메시지 처리
//        for (Message message : unreadMessages) {
//            message.addReadByUser(user); // 읽은 사용자 추가
//            message.setIsRead(true); // 메시지 읽음 처리
//            message.setReadByUsersCount(message.getReadByUsers().size()); // 읽은 사용자 수 업데이트
//            System.out.println("Marking message as read: " + message.getId()); // 디버깅 로그
//        }
//
//        // 변경사항 저장
//        messageRepository.saveAll(unreadMessages);
//        System.out.println("Saved " + unreadMessages.size() + " unread messages as read."); // 디버깅 로그
//    }




    // 메시지 조회
//    @Transactional
//    public List<ChatMessage> getMessagesByChatRoomWithReadUpdate(Long chatRoomId, Long userId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
//        Page<Message> dbMessages = messageRepository.findByChatRoomId(chatRoomId, pageable);
//
//        // 사용자 객체 가져오기
//        User user = userService.getUserById(userId);
//
//        // 메시지 읽음 처리 및 변환
//        List<Message> updatedMessages = dbMessages.stream()
//                .peek(message -> {
//                    // 내가 보낸 메시지는 읽음 처리 안함
//                    if (!message.getSender().getId().equals(userId) && !message.getReadByUsers().contains(user)) {
//                        message.addReadByUser(user); // 읽은 사용자 추가
//                        message.setIsRead(true); // 메시지 읽음 상태 설정
//                        message.setReadByUsersCount(message.getReadByUsers().size());
//                    }
//                })
//                .collect(Collectors.toList());
//
//        // 변경된 메시지 저장
//        messageRepository.saveAll(updatedMessages);
//
//        // **실시간 저장된 메시지를 추가로 포함**
//        if (!updatedMessages.isEmpty()) {
//            Message latestMessage = updatedMessages.get(0);
//            if (latestMessage.getSentAt().isAfter(LocalDateTime.now().minusSeconds(1))) {
//                updatedMessages.add(latestMessage);
//            }
//        }
//
//        // ChatMessage로 변환
//        return updatedMessages.stream()
//                .map(message -> new ChatMessage(
//                        message.getId(),
//                        MessageType.TALK,
//                        message.getChatRoom().getId().toString(),
//                        message.getSender().getId(),
//                        message.getSender().getName(),
//                        message.getContent(),
//                        message.getSentAt(),
//                        true,
//                        message.getReadByUsersCount()
//                ))
//                .collect(Collectors.toList());
//    }

    public List<ChatMessage> getMessagesByChatRoomWithReadUpdate(Long chatRoomId, Long userId, int page, int size) {
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);

        // Redis에서 메시지 가져오기
        List<Object> messages = redisTemplate.opsForList().range(chatRoomMessagesKey, page * size, (page + 1) * size - 1);
        if (messages == null) return List.of();

        System.out.println("message : + " +messages);

        // 메시지 읽음 처리
        messages.forEach(messageObj -> {
            ChatMessage message = (ChatMessage) messageObj;

            // 내가 보낸 메시지는 읽음 처리 안함
            if (!message.getSenderId().equals(userId)) {
                message.setRead(true);
//                message.setReadByUsersCount(message.getReadByUsersCount() + 1); // 읽은 사용자 수 증가
            }
        });

        System.out.println("message : + " +messages);

        return messages.stream().map(obj -> (ChatMessage) obj).collect(Collectors.toList());
    }



    // 최신 메시지 조회
    public String getLatestMessageContent(Long chatRoomId) {
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        return (String) redisTemplate.opsForValue().get(latestMessageKey);
    }

    // 읽지 않은 메시지 처리
    public void updateReadStatus(Long chatRoomId, Long userId) {
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);

        List<Object> messages = redisTemplate.opsForList().range(chatRoomMessagesKey, 0, -1);
        if (messages == null) return;

        messages.forEach(messageObj -> {
            ChatMessage message = (ChatMessage) messageObj;

            // 내가 보낸 메시지가 아니고 읽음 상태가 아니면 처리
            if (!message.getSenderId().equals(userId) && !message.isRead()) {
                message.setRead(true);
                message.setReadByUsersCount(message.getReadByUsersCount() + 1); // 읽은 사용자 수 증가
            }
        });

        // Redis에 다시 저장 (리스트를 업데이트)
        redisTemplate.delete(chatRoomMessagesKey);
        redisTemplate.opsForList().rightPushAll(chatRoomMessagesKey, messages);
    }

}
