package com.example.chating.Service;

import com.example.chating.Repository.MessageRepository;
import com.example.chating.Dto.ChatMessage;
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
    public ChatMessage saveMessage(Long chatRoomId, Long senderId, String content) {
        if (chatRoomId == null || senderId == null) {
            throw new IllegalArgumentException("Chat Room ID or Sender ID must not be null.");
        }

        String senderName = userService.getUserNameById(senderId);

        // 데이터베이스에 저장
        Message messageEntity = Message.builder()
                .chatRoom(chatRoomService.getChatRoomById(chatRoomId))
                .sender(userService.getUserById(senderId))
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(messageEntity);

        // Redis 캐시 업데이트 (최신 메시지, 활동 시간)
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        redisTemplate.opsForValue().set(latestMessageKey, content);
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());

        return new ChatMessage(
                messageEntity.getId(),
                ChatMessage.MessageType.TALK,
                chatRoomId.toString(),
                senderId,
                senderName,
                content,
                messageEntity.getSentAt()
        );
    }




    /**
     * 특정 채팅방 내 모든 메시지를 읽음 처리
     * - 사용자가 특정 채팅방 내 모든 메시지를 읽음 처리
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void markAllMessagesAsRead(Long roomId, Long userId) {
        // 해당 채팅방의 모든 메시지 조회
        List<Message> messages = messageRepository.findByChatRoomId(roomId, Pageable.unpaged()).getContent();

        if (messages.isEmpty()) {
            return; // 메시지가 없으면 종료
        }

        User user = userService.getUserById(userId);

        // 모든 메시지를 읽음 처리
        messages.forEach(message -> {
            if (!message.getReadByUsers().contains(user)) {
                message.addReadByUser(user); // 읽은 사용자 추가
                message.setIsRead(true); // 읽음 상태 설정
                message.setReadByUsersCount(message.getReadByUsers().size());
            }
        });

        // 일괄 저장
        messageRepository.saveAll(messages);
    }


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
                    // 읽음 처리
                    if (!message.getReadByUsers().contains(user)) {
                        message.addReadByUser(user); // 읽은 사용자 추가
                        message.setIsRead(true); // 메시지 읽음 상태 설정
                        message.setReadByUsersCount(message.getReadByUsers().size());
                    }
                })
                .collect(Collectors.toList());

        // 변경된 메시지 저장
        messageRepository.saveAll(updatedMessages);

        // ChatMessage로 변환
        return updatedMessages.stream()
                .map(message -> new ChatMessage(
                        message.getId(),
                        ChatMessage.MessageType.TALK,
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
