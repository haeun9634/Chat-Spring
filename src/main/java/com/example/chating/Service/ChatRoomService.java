package com.example.chating.Service;

import com.example.chating.Repository.ChatRoomRepository;
import com.example.chating.Repository.MessageRepository;
import com.example.chating.Repository.UserChatRoomRepository;
import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.UserChatRoom;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
        // 1. 연관된 메시지 삭제
        List<Message> messages = messageRepository.findByChatRoomId(chatRoomId);
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }

        // 2. 연관된 UserChatRoom 삭제 및 Redis 업데이트
        List<UserChatRoom> userChatRooms = userChatRoomRepository.findByChatRoomId(chatRoomId);
        if (!userChatRooms.isEmpty()) {
            // Redis에서 사용자-채팅방 매핑 제거
            for (UserChatRoom userChatRoom : userChatRooms) {
                Long userId = userChatRoom.getUser().getId();
                String userChatRoomsKey = "user:" + userId + ":chatrooms";
                redisTemplate.opsForSet().remove(userChatRoomsKey, chatRoomId);
            }
            // DB에서 연관된 UserChatRoom 삭제
            userChatRoomRepository.deleteAll(userChatRooms);
        }

        // 3. Redis에서 채팅방 삭제
        redisTemplate.opsForHash().delete(CHAT_ROOMS_KEY, chatRoomId.toString());

        // 4. Redis의 채팅방 활동 데이터 삭제
        redisTemplate.opsForZSet().remove(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString());

        // 5. DB에서 채팅방 삭제
        if (chatRoomRepository.existsById(chatRoomId)) {
            chatRoomRepository.deleteById(chatRoomId);
        } else {
            throw new IllegalArgumentException("ChatRoom not found in the database");
        }
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


    private static final String CHAT_ROOM_ACTIVITY_KEY = "chatroom:activity";  // 채팅방 활동 시간을 저장하는 ZSet 키

    // 채팅방 최신 활동 시간 업데이트
    public void updateChatRoomActivity(Long chatRoomId) {
        redisTemplate.opsForZSet().add(CHAT_ROOM_ACTIVITY_KEY, chatRoomId.toString(), System.currentTimeMillis());
    }

    // 사용자 채팅방 목록 (최신 활동 기준 정렬)
    public List<ChatRoom> getChatRoomsByUser(Long userId) {
        String userChatRoomsKey = "user:" + userId + ":chatrooms"; // 사용자가 참여한 채팅방 리스트 키 생성

        // Redis에서 사용자가 참여한 채팅방 ID 가져오기
        Set<Object> userChatRoomIdsSet = redisTemplate.opsForSet().members(userChatRoomsKey);

        // Redis에 데이터가 없으면 DB에서 조회
        if (userChatRoomIdsSet == null || userChatRoomIdsSet.isEmpty()) {
            List<UserChatRoom> userChatRooms = userChatRoomRepository.findByUserId(userId);
            if (userChatRooms.isEmpty()) {
                return List.of(); // 빈 리스트 반환
            }

            // DB에서 조회한 채팅방 ID를 Redis에 저장
            userChatRoomIdsSet = userChatRooms.stream()
                    .map(userChatRoom -> userChatRoom.getChatRoom().getId())
                    .collect(Collectors.toSet());

            userChatRoomIdsSet.forEach(chatRoomId -> redisTemplate.opsForSet().add(userChatRoomsKey, chatRoomId));
        }

        // Set<Object>를 List<Long>으로 변환
        List<Long> userChatRoomIds = userChatRoomIdsSet.stream()
                .map(id -> Long.valueOf(id.toString()))  // Object -> Long 변환
                .collect(Collectors.toList());

        // DB에서 유효한 채팅방 ID 필터링
        List<Long> validChatRoomIds = userChatRoomIds.stream()
                .filter(chatRoomRepository::existsById)  // DB에 존재하는 채팅방만 필터링
                .collect(Collectors.toList());

        System.out.println("사용자가 참여한 유효한 채팅방 ID: " + validChatRoomIds);

        // 최신 활동 기준으로 정렬된 채팅방 ID 가져오기
        Set<Object> sortedChatRoomIdsSet = redisTemplate.opsForZSet()
                .reverseRangeByScore(CHAT_ROOM_ACTIVITY_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        List<Long> sortedChatRoomIds = (sortedChatRoomIdsSet == null || sortedChatRoomIdsSet.isEmpty())
                ? List.of()
                : sortedChatRoomIdsSet.stream().map(id -> Long.valueOf(id.toString())).collect(Collectors.toList());

        System.out.println("정렬된 채팅방 ID: " + sortedChatRoomIds);

        // 최종 정렬된 채팅방 ID 리스트 생성
        List<Long> finalChatRoomIds = new ArrayList<>();
        sortedChatRoomIds.stream().filter(validChatRoomIds::contains).forEach(finalChatRoomIds::add);
        validChatRoomIds.stream().filter(id -> !finalChatRoomIds.contains(id)).forEach(finalChatRoomIds::add);

        System.out.println("최종 정렬된 채팅방 ID: " + finalChatRoomIds);

        // 최종 정렬된 채팅방 ID를 기반으로 채팅방 객체 가져오기
        return finalChatRoomIds.stream()
                .map(chatRoomRepository::findById)  // DB에서 채팅방 조회
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }




    // 채팅방 ID로 채팅방 정보 조회
    public ChatRoom getChatRoomById(Long chatRoomId) {
        // Redis에서 채팅방 정보 조회
        Object chatRoomObj = redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, chatRoomId.toString());
        if (chatRoomObj instanceof ChatRoom) {
            return (ChatRoom) chatRoomObj;
        }

        // Redis에 없으면 DB에서 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));

        // DB에서 가져온 데이터를 Redis에 저장
        redisTemplate.opsForHash().put(CHAT_ROOMS_KEY, chatRoomId.toString(), chatRoom);

        return chatRoom;
    }


}
