package com.example.chating.Service;

import com.example.chating.Repository.MessageRepository;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.chat.Message;
import lombok.RequiredArgsConstructor;
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
        ChatMessage chatMessage = new ChatMessage(
                ChatMessage.MessageType.TALK, chatRoomId.toString(),
                senderId, senderName, content, LocalDateTime.now()
        );
        String redisKey = "chatroom:" + chatRoomId + ":messages";
        redisTemplate.opsForZSet().add(redisKey, chatMessage, System.currentTimeMillis());

        // 데이터베이스에 저장
        Message messageEntity = Message.builder()
                .chatRoom(chatRoomService.getChatRoomById(chatRoomId))
                .sender(userService.getUserById(senderId))
                .content(content)
                .sentAt(chatMessage.getSendAt())
                .build();

        messageRepository.save(messageEntity);
    }



    /**
     * 특정 채팅방 내 모든 메시지를 읽음 처리
     * - 사용자가 특정 채팅방 내 모든 메시지를 읽음 처리
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    // 메시지 읽음 처리
    public void markAllMessagesAsRead(Long roomId, Long userId) {
        // 해당 채팅방의 모든 메시지 조회 (페이징 없이 모든 메시지 가져오기)
        Page<Message> pageMessages = messageRepository.findByChatRoomId(roomId, Pageable.unpaged());
        List<Message> messages = pageMessages.getContent(); // getContent()로 List<Message>로 변환

        // Redis 키 생성 (채팅방의 메시지 목록)
        String chatRoomMessagesKey = "chatroom:" + roomId + ":messages";
        Set<Object> redisMessages = redisTemplate.opsForZSet().range(chatRoomMessagesKey, 0, -1);

        // 모든 메시지 읽음 처리
        for (Message message : messages) {
            // 읽은 사용자 추가 및 읽음 처리
            message.addReadByUser(userService.getUserById(userId));
            message.setIsRead(true);

            // 읽은 사용자 수 업데이트
            int readByUsersCount = message.getReadByUsers().size();
            message.setReadByUsersCount(readByUsersCount);

            // 데이터베이스에 저장
            messageRepository.save(message);

            // Redis에서 읽은 사용자 정보 업데이트
            String messageKey = "message:" + message.getId() + ":readBy";
            redisTemplate.opsForSet().add(messageKey, userId);
        }

        // Redis에 있는 메시지들도 읽음 처리
        if (redisMessages != null && !redisMessages.isEmpty()) {
            redisMessages.stream()
                    .filter(obj -> obj instanceof ChatMessage)
                    .map(obj -> (ChatMessage) obj)
                    .forEach(chatMessage -> {
                        // 읽은 사용자 수 계산
                        String messageKey = "message:" + chatMessage.getRoomId() + ":readBy";
                        redisTemplate.opsForSet().add(messageKey, userId);

                        Set<Object> readByUsers = redisTemplate.opsForSet().members(messageKey);
                        int readByUsersCount = (readByUsers != null) ? readByUsers.size() : 0;

                        // ChatMessage 업데이트
                        chatMessage.setReadByUsersCount(readByUsersCount);
                        chatMessage.setIsRead(true);

                        // Redis에 업데이트된 ChatMessage 저장
                        redisTemplate.opsForZSet().add(chatRoomMessagesKey, chatMessage, System.currentTimeMillis());
                    });
        }
    }



    // 메시지 읽음 처리
    public void markMessageAsRead(Long messageId, Long userId) {
        // 메시지를 읽은 사용자 추가
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        User user = userService.getUserById(userId);

        // 메시지를 읽은 사용자 추가
        message.addReadByUser(user);
        message.setIsRead(true);  // 메시지 읽음 처리

        messageRepository.save(message); // 변경된 메시지를 저장

        // Redis에서 읽은 사용자 정보 업데이트
        String key = "message:" + messageId + ":readBy";

        // Redis에 사용자 ID를 추가하고, 읽은 사용자 수 갱신
        redisTemplate.opsForSet().add(key, userId);

        // Redis에 저장된 읽은 사용자 수를 가져와서 카운트 값 갱신
        Set<Object> readByUsers = redisTemplate.opsForSet().members(key);
        int readByUsersCount = (readByUsers != null) ? readByUsers.size() : 0;

        // 읽은 사용자 수를 ChatMessage 객체에 업데이트
        message.setReadByUsersCount(readByUsersCount);
    }

    //메시지 조회
    public List<ChatMessage> getMessagesByChatRoom(Long chatRoomId, int page, int size) {
        String redisKey = "chatroom:" + chatRoomId + ":messages";
        Set<Object> redisMessages = redisTemplate.opsForZSet().range(redisKey, 0, -1);

        // Redis에 데이터가 있는 경우
        if (redisMessages != null && !redisMessages.isEmpty()) {
            return redisMessages.stream()
                    .map(obj -> (ChatMessage) obj)
                    .map(chatMessage -> {
                        // 읽은 사용자 수 계산
                        String key = "message:" + chatMessage.getRoomId() + ":readBy";
                        Set<Object> readByUsers = redisTemplate.opsForSet().members(key);
                        int readByUsersCount = (readByUsers != null) ? readByUsers.size() : 0;
                        chatMessage.setReadByUsersCount(readByUsersCount);

                        // 메시지 읽음 여부 확인 (redis나 db에서 값을 확인하여 설정)
                        chatMessage.setIsRead(readByUsersCount > 0); // 예: 읽은 사용자 수가 0보다 크면 true로 설정
                        return chatMessage;
                    })
                    .collect(Collectors.toList());
        }

        // Redis에 데이터가 없는 경우 -> 데이터베이스에서 페이징 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> dbMessages = messageRepository.findByChatRoomId(chatRoomId, pageable);

        return dbMessages.stream()
                .map(message -> {
                    int readByUsersCount = message.getReadByUsers().size(); // 데이터베이스에서 읽은 사용자 수 계산
                    boolean isRead = readByUsersCount > 0; // 읽음 여부 설정

                    return new ChatMessage(
                            ChatMessage.MessageType.TALK,
                            message.getChatRoom().getId().toString(),
                            message.getSender().getId(),
                            message.getSender().getName(), // 사용자 이름
                            message.getContent(),
                            message.getSentAt(),
                            readByUsersCount, // 읽은 사용자 수
                            isRead // 읽음 상태 추가
                    );
                })
                .collect(Collectors.toList());
    }




}
