package com.example.chating.Service;

import com.example.chating.domain.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_ROOMS_KEY = "chatrooms";

    // 채팅방 생성
    public ChatRoom createChatRoom(String name) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);

        // 채팅방 ID 생성
        Long id = redisTemplate.opsForValue().increment("chatroom:id:counter");
        chatRoom.setId(id);

        // Redis에 채팅방 데이터 저장
        redisTemplate.opsForHash().put(CHAT_ROOMS_KEY, id.toString(), chatRoom);
        System.out.println("ChatRoom saved to Redis: " + chatRoom);

        // 저장된 데이터 확인
        Object savedData = redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, id.toString());
        System.out.println("Retrieved from Redis: " + savedData);

        return chatRoom;
    }



    // 채팅방 삭제
    public void deleteChatRoom(Long chatRoomId) {
        redisTemplate.opsForHash().delete(CHAT_ROOMS_KEY, chatRoomId.toString());
    }

    // 사용자 추가
    public void addUserToChatRoom(Long chatRoomId, Long userId) {
        // 사용자 -> 참여 채팅방 매핑 데이터 저장
        String userChatRoomsKey = "user:" + userId + ":chatrooms";
        redisTemplate.opsForSet().add(userChatRoomsKey, chatRoomId);

        // 채팅방 -> 참여 사용자 매핑 데이터 저장
        String chatRoomUsersKey = "chatroom:" + chatRoomId + ":users";
        redisTemplate.opsForSet().add(chatRoomUsersKey, userId);

        // 확인 로그 추가
        System.out.println("User " + userId + " added to chat room " + chatRoomId);
    }


    // 사용자 제거
    public void removeUserFromChatRoom(Long chatRoomId, Long userId) {
        String userChatRoomsKey = "user:" + userId + ":chatrooms";
        String chatRoomUsersKey = "chatroom:" + chatRoomId + ":users";

        // 사용자와 채팅방 간 매핑 데이터 제거
        redisTemplate.opsForSet().remove(userChatRoomsKey, chatRoomId);
        redisTemplate.opsForSet().remove(chatRoomUsersKey, userId);
    }

    // 사용자 채팅방 목록
    public List<ChatRoom> getChatRoomsByUser(Long userId) {
        String userChatRoomsKey = "user:" + userId + ":chatrooms";
        Set<Object> chatRoomIds = redisTemplate.opsForSet().members(userChatRoomsKey);

        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            System.out.println("No chat rooms found for user: " + userId);
            return List.of();
        }

        return chatRoomIds.stream()
                .map(id -> {
                    Object chatRoomObj = redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, id.toString());
                    System.out.println("Fetched ChatRoom from Redis: " + chatRoomObj);
                    if (chatRoomObj instanceof ChatRoom) {
                        return (ChatRoom) chatRoomObj;
                    } else {
                        System.out.println("Invalid ChatRoom data for ID: " + id);
                        return null;
                    }
                })
                .filter(chatRoom -> chatRoom != null)
                .collect(Collectors.toList());
    }


}
