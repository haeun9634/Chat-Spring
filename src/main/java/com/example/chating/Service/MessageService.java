package com.example.chating.Service;

import com.example.chating.Repository.MessageRepository;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.MessageType;
import com.example.chating.domain.chat.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final ObjectMapper objectMapper;

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
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);

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

        // Redis에 저장하기 전 중복 여부 확인
        try {
            String chatMessageJson = objectMapper.writeValueAsString(chatMessage);
            long score = chatMessage.getSendAt().toEpochSecond(ZoneOffset.UTC);

            // 중복 메시지 방지: 메시지가 이미 Redis에 존재하는지 검사
            Set<Object> existingMessages = redisTemplate.opsForZSet().rangeByScore(chatRoomMessagesKey, score, score);
            if (existingMessages != null && existingMessages.contains(chatMessageJson)) {
                System.out.println("Duplicate message detected, skipping save.");
                return chatMessage; // 중복 메시지는 저장하지 않음
            }

            // Redis에 ZSet으로 메시지 저장
            redisTemplate.opsForZSet().add(chatRoomMessagesKey, chatMessageJson, score);

        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize ChatMessage: " + e.getMessage());
            throw new RuntimeException("Failed to serialize ChatMessage", e);
        }

        // 최신 메시지 및 활동 시간 업데이트
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        redisTemplate.opsForValue().set(latestMessageKey, content);
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());

        return chatMessage;
    }

    public List<ChatMessage> getMessagesByChatRoomWithReadUpdate(Long chatRoomId, Long userId, int page, int size) {
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);

        // ZSet에서 메시지 가져오기 (페이징 처리)
        int start = page * size;
        int end = start + size - 1;

        Set<Object> messageSet = redisTemplate.opsForZSet().range(chatRoomMessagesKey, start, end);
        if (messageSet == null || messageSet.isEmpty()) return List.of();

        // 메시지 읽음 처리 및 역직렬화
        return messageSet.stream()
                .map(messageObj -> {
                    try {
                        ChatMessage message = objectMapper.readValue(messageObj.toString(), ChatMessage.class);

                        // 내가 보낸 메시지는 읽음 처리 제외
                        if (!message.getSenderId().equals(userId) && !message.isRead()) {
                            message.setRead(true);
                            message.setReadByUsersCount(message.getReadByUsersCount() + 1);

                            // Redis 업데이트
                            String updatedMessageJson = objectMapper.writeValueAsString(message);
                            long score = message.getSendAt().toEpochSecond(ZoneOffset.UTC);
                            redisTemplate.opsForZSet().add(chatRoomMessagesKey, updatedMessageJson, score);
                        }

                        return message;
                    } catch (JsonProcessingException e) {
                        System.err.println("Failed to deserialize message: " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toList());
    }



    // 최신 메시지 조회
    public String getLatestMessageContent(Long chatRoomId) {
        String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
        return (String) redisTemplate.opsForValue().get(latestMessageKey);
    }

    // 읽지 않은 메시지 처리
    public void updateReadStatus(Long chatRoomId, Long userId) {
        String chatRoomMessagesKey = String.format(CHAT_ROOM_MESSAGES_KEY, chatRoomId);
        String readUsersKey = "chatroom:" + chatRoomId + ":readUsers";

        // Redis에서 ZSet 메시지 가져오기
        Set<Object> messageSet = redisTemplate.opsForZSet().range(chatRoomMessagesKey, 0, -1);
        if (messageSet == null || messageSet.isEmpty()) return;

        // 메시지 읽음 처리
        messageSet.forEach(messageObj -> {
            if (messageObj instanceof String) {
                try {
                    // 메시지 역직렬화
                    ChatMessage message = objectMapper.readValue((String) messageObj, ChatMessage.class);

                    // 내가 보낸 메시지는 읽음 처리 제외
                    if (!message.getSenderId().equals(userId) && !message.isRead()) {
                        message.setRead(true);
                        message.setReadByUsersCount(message.getReadByUsersCount() + 1);

                        // 기존 메시지를 ZSet에서 삭제
                        redisTemplate.opsForZSet().remove(chatRoomMessagesKey, messageObj);

                        // 업데이트된 메시지를 다시 ZSet에 저장
                        String updatedMessageJson = objectMapper.writeValueAsString(message);
                        long score = message.getSendAt().toEpochSecond(ZoneOffset.UTC);
                        redisTemplate.opsForZSet().add(chatRoomMessagesKey, updatedMessageJson, score);

                        // Redis Set에 사용자 ID 추가 (읽음 처리 사용자)
                        redisTemplate.opsForSet().add(readUsersKey, userId.toString());
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("Failed to deserialize/update message: " + e.getMessage());
                }
            } else {
                System.err.println("Unexpected message format: " + messageObj);
            }
        });
    }


    public int getReadByUsersCount(Long roomId) {
        try {
            String redisKey = "chatroom:" + roomId + ":readUsers";
            Long count = redisTemplate.opsForSet().size(redisKey);

            // Null 값 방지
            int result = (count != null) ? count.intValue() : 0;
            //System.out.println("Read by users count: roomId=" + roomId + ", count=" + result);
            return result;
        } catch (Exception e) {
            System.err.println("Failed to get read by users count: roomId=" + roomId);
            e.printStackTrace();
            return 0;
        }
    }



}
