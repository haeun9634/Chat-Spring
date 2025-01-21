package com.example.chating.Service;

import com.example.chating.Dto.ChatRoomDto;
import com.example.chating.Dto.UserProfileDto;
import com.example.chating.Repository.ChatRoomRepository;
import com.example.chating.Repository.MessageRepository;
import com.example.chating.Repository.UserChatRoomRepository;
import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.UserChatRoom;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomRepository chatRoomRepository; // DB 저장소
    private final UserService userService;
    private final UserChatRoomRepository userChatRoomRepository;
    private final MessageRepository messageRepository;

    private static final String CHAT_ROOMS_KEY = "chatrooms";
    private static final String CHAT_ROOM_ACTIVITY_KEY = "chatroom:activity";
    private static final String CHAT_ROOM_LATEST_MESSAGE_KEY = "chatroom:%s:latestMessage";
    // 채팅방 생성
    @Transactional
    public ChatRoom createChatRoom(String name) {
        // 1. 새로운 ChatRoom 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();

        // 2. DB 저장
        chatRoom = chatRoomRepository.save(chatRoom);

        // 3. Redis 저장
        redisTemplate.opsForHash().put(CHAT_ROOMS_KEY, chatRoom.getId().toString(), chatRoom);

        return chatRoom;
    }


    // 채팅방 삭제
    @Transactional
    public void deleteChatRoom(Long chatRoomId) {
        // 연관된 메시지 삭제
        List<Message> messages = messageRepository.findByChatRoomId(chatRoomId);
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }

        // 연관된 UserChatRoom 삭제
        List<UserChatRoom> userChatRooms = userChatRoomRepository.findByChatRoomId(chatRoomId);
        if (!userChatRooms.isEmpty()) {
            userChatRooms.forEach(userChatRoom -> {
                String userChatRoomsKey = "user:" + userChatRoom.getUser().getId() + ":chatrooms";
                redisTemplate.opsForSet().remove(userChatRoomsKey, chatRoomId);
            });
            userChatRoomRepository.deleteAll(userChatRooms);
        }

        // Redis에서 채팅방 삭제
        redisTemplate.opsForHash().delete(CHAT_ROOMS_KEY, chatRoomId.toString());

        // Redis의 활동 시간 데이터 삭제
        redisTemplate.opsForZSet().remove(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString());

        chatRoomRepository.deleteById(chatRoomId);

    }




    // 사용자 추가
    public void addUserToChatRoom(Long chatRoomId, Long userId) {
        // 사용자 -> 참여 채팅방 매핑 데이터 저장
        String userChatRoomsKey = "user:" + userId + ":chatrooms";
        redisTemplate.opsForSet().add(userChatRoomsKey, chatRoomId);

        // 채팅방 -> 참여 사용자 매핑 데이터 저장
        String chatRoomUsersKey = "chatroom:" + chatRoomId + ":users";
        redisTemplate.opsForSet().add(chatRoomUsersKey, userId);

        // DB 저장
        User user = userService.getUserById(userId);
        ChatRoom chatRoom = getChatRoomById(chatRoomId);

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .user(user)
                .chatRoom(chatRoom)
                .build();

        userChatRoomRepository.save(userChatRoom);

        // 확인 로그 추가
        System.out.println("User " + userId + " added to chat room " + chatRoomId);
    }

    // 사용자 제거
    public void removeUserFromChatRoom(Long chatRoomId, Long userId) {
        String userChatRoomsKey = "user:" + userId + ":chatrooms";
        String chatRoomUsersKey = "chatroom:" + chatRoomId + ":users";

        // 사용자와 채팅방 간 매핑 데이터 제거 (Redis)
        redisTemplate.opsForSet().remove(userChatRoomsKey, chatRoomId);
        redisTemplate.opsForSet().remove(chatRoomUsersKey, userId);

        // DB에서 제거
        User user = userService.getUserById(userId);
        ChatRoom chatRoom = getChatRoomById(chatRoomId);

        UserChatRoom userChatRoom = userChatRoomRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new IllegalArgumentException("UserChatRoom not found"));

        userChatRoomRepository.delete(userChatRoom);
    }

    // 채팅방 최신 활동 시간 업데이트
    public void updateChatRoomActivity(Long chatRoomId) {
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());
    }

    // 사용자 채팅방 목록 (최신 활동 기준 정렬)
    public List<ChatRoomDto> getChatRoomsByUser(Long userId) {
        String userChatRoomsKey = "user:" + userId + ":chatrooms";

        // Redis에서 참여 채팅방 ID 가져오기 및 변환
        Set<Long> userChatRoomIdsSet = redisTemplate.opsForSet()
                .members(userChatRoomsKey)
                .stream()
                .map(obj -> {
                    String str = obj.toString();
                    return Long.valueOf(str.replaceAll("\\[\"java.lang.Long\",", "").replaceAll("]", "").trim());
                })
                .collect(Collectors.toSet());

        if (userChatRoomIdsSet == null || userChatRoomIdsSet.isEmpty()) {
            List<UserChatRoom> userChatRooms = userChatRoomRepository.findByUserId(userId);
            userChatRoomIdsSet = userChatRooms.stream()
                    .map(userChatRoom -> userChatRoom.getChatRoom().getId())
                    .collect(Collectors.toSet());
            userChatRoomIdsSet.forEach(chatRoomId -> redisTemplate.opsForSet().add(userChatRoomsKey, chatRoomId));
        }

        // Redis ZSet에서 최신 활동 기준으로 정렬된 채팅방 ID 가져오기
        List<Long> sortedChatRoomIds = redisTemplate.opsForZSet()
                .reverseRange(CHAT_ROOM_ACTIVITY_KEY, 0, -1)
                .stream()
                .map(Object::toString)
                .map(Long::valueOf)
                .filter(userChatRoomIdsSet::contains) // 참여 중인 채팅방만 포함
                .collect(Collectors.toList());

        // 정렬된 목록에 없는 채팅방 추가
        userChatRoomIdsSet.stream()
                .filter(id -> !sortedChatRoomIds.contains(id)) // 정렬되지 않은 채팅방 추가
                .forEach(sortedChatRoomIds::add);

        System.out.println("Redis userChatRoomIdsSet: " + userChatRoomIdsSet);
        System.out.println("Sorted chatRoomIds: " + sortedChatRoomIds);

        return sortedChatRoomIds.stream()
                .map(chatRoomId -> {
                    ChatRoom chatRoom = getChatRoomById(chatRoomId);

                    // 최신 메시지 Redis에서 가져오기
                    String latestMessageKey = String.format(CHAT_ROOM_LATEST_MESSAGE_KEY, chatRoomId);
                    String latestMessage = (String) redisTemplate.opsForValue().get(latestMessageKey);
                    if (latestMessage == null) {
                        latestMessage = getLatestMessageContentFromDb(chatRoomId);
                        redisTemplate.opsForValue().set(latestMessageKey, latestMessage);
                    }

                    List<UserProfileDto> userProfiles = getUserProfilesByChatRoomId(chatRoomId);
                    return new ChatRoomDto(chatRoom, latestMessage, userProfiles);
                })
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



    public List<UserProfileDto> getUserProfilesByChatRoomId(Long chatRoomId) {
        List<User> users = userChatRoomRepository.findUsersByChatRoomId(chatRoomId);

        return users.stream()
                .map(user -> new UserProfileDto(
                        user.getId(),
                        user.getName(),
                        user.getEmoji() // 프로필 이미지 필드
                ))
                .collect(Collectors.toList());
    }



    //채팅방에 있는 사용자 조회
    public List<User> getUserByRoomId(Long roomId){
        return userChatRoomRepository.findUsersByChatRoomId(roomId);
    }


    // 채팅방 ID로 채팅방 정보 조회
    public ChatRoom getChatRoomById(Long chatRoomId) {
        Object chatRoomObj = redisTemplate.opsForHash().get("chatrooms", chatRoomId.toString());
        if (chatRoomObj instanceof ChatRoom) {
            return (ChatRoom) chatRoomObj;
        }
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));
        redisTemplate.opsForHash().put("chatrooms", chatRoomId.toString(), chatRoom);
        return chatRoom;
    }


}
