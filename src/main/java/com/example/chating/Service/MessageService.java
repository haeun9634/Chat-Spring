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

    private static final String CHAT_ROOM_ACTIVITY_KEY = "chatroom:activity";
    private static final String CHAT_ROOM_LATEST_MESSAGE_KEY = "chatroom:%s:latestMessage";

    // 메시지 저장
    @Transactional
    public ChatMessage saveMessage(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        if (chatRoomId == null || senderId == null) {
            throw new IllegalArgumentException("Chat Room ID or Sender ID must not be null.");
        }

        String senderName = userService.getUserNameById(senderId);

        // 메시지 저장
        Message messageEntity = Message.builder()
                .chatRoom(chatRoomService.getChatRoomById(chatRoomId))
                .sender(userService.getUserById(senderId))
                .content(content)
                .messageType(messageType)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(messageEntity);

        // Redis 업데이트를 트랜잭션 내에서 동기적으로 처리
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        redisTemplate.opsForValue().set(latestMessageKey, content);
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());
        //System.out.println("Redis 캐시 업데이트 완료");

        return new ChatMessage(
                messageEntity.getId(),
                messageType,
                chatRoomId.toString(),
                senderId,
                senderName,
                content,
                messageEntity.getSentAt()
        );
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
    @Transactional
    public List<ChatMessage> getMessagesByChatRoomWithReadUpdate(Long chatRoomId, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> dbMessages = messageRepository.findByChatRoomId(chatRoomId, pageable);

        // 사용자 객체 가져오기
        User user = userService.getUserById(userId);

        // 메시지 읽음 처리 및 변환
        List<Message> updatedMessages = dbMessages.stream()
                .peek(message -> {
                    // 내가 보낸 메시지는 읽음 처리 안함
                    if (!message.getSender().getId().equals(userId) && !message.getReadByUsers().contains(user)) {
                        message.addReadByUser(user); // 읽은 사용자 추가
                        message.setIsRead(true); // 메시지 읽음 상태 설정
                        message.setReadByUsersCount(message.getReadByUsers().size());
                    }
                })
                .collect(Collectors.toList());

        // 변경된 메시지 저장
        messageRepository.saveAll(updatedMessages);

        // **실시간 저장된 메시지를 추가로 포함**
        if (!updatedMessages.isEmpty()) {
            Message latestMessage = updatedMessages.get(0);
            if (latestMessage.getSentAt().isAfter(LocalDateTime.now().minusSeconds(1))) {
                updatedMessages.add(latestMessage);
            }
        }

        // ChatMessage로 변환
        return updatedMessages.stream()
                .map(message -> new ChatMessage(
                        message.getId(),
                        MessageType.TALK,
                        message.getChatRoom().getId().toString(),
                        message.getSender().getId(),
                        message.getSender().getName(),
                        message.getContent(),
                        message.getSentAt(),
                        true,
                        message.getReadByUsersCount()
                ))
                .collect(Collectors.toList());
    }



    public String getLatestMessageContentFromDb(Long chatRoomId) {
        Pageable pageable = PageRequest.of(0, 1); // 최신 메시지 하나만 가져옴
        List<Message> messages = messageRepository.findLatestMessageByChatRoomId(chatRoomId, pageable);

        return messages.stream()
                .findFirst()
                .map(Message::getContent) // 메시지 내용만 반환
                .orElse(null);
    }



}
