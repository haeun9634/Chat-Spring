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

        // DB에서 채팅방 삭제
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
        String userChatRoomsKey = "user:" + userId + ":chatrooms";

        // Redis에서 사용자가 참여한 채팅방 ID 가져오기
        Set<Object> userChatRoomIdsSet = redisTemplate.opsForSet().members(userChatRoomsKey);

        // Redis에 데이터가 없으면 DB에서 조회
        if (userChatRoomIdsSet == null || userChatRoomIdsSet.isEmpty()) {
            List<UserChatRoom> userChatRooms = userChatRoomRepository.findByUserId(userId);
            if (userChatRooms.isEmpty()) {
                return List.of();
            }

            // DB에서 조회한 채팅방 ID를 Redis에 저장
            userChatRoomIdsSet = userChatRooms.stream()
                    .map(userChatRoom -> userChatRoom.getChatRoom().getId())
                    .collect(Collectors.toSet());

            userChatRoomIdsSet.forEach(chatRoomId -> redisTemplate.opsForSet().add(userChatRoomsKey, chatRoomId));
        }

        // Redis에서 채팅방 객체 가져오기
        List<Long> chatRoomIds = userChatRoomIdsSet.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());

        // DB와 Redis 간 데이터 동기화 검증
        List<Long> validChatRoomIds = chatRoomIds.stream()
                .filter(chatRoomRepository::existsById)  // DB에 존재하는 채팅방만 유지
                .collect(Collectors.toList());

        // 유효하지 않은 Redis 데이터 제거
        chatRoomIds.stream()
                .filter(id -> !validChatRoomIds.contains(id))
                .forEach(id -> redisTemplate.opsForSet().remove(userChatRoomsKey, id));

        // 최신 활동 기준으로 정렬된 채팅방 ID 가져오기
        Set<Object> sortedChatRoomIdsSet = redisTemplate.opsForZSet()
                .reverseRangeByScore(CHAT_ROOM_ACTIVITY_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        List<Long> sortedChatRoomIds = (sortedChatRoomIdsSet == null || sortedChatRoomIdsSet.isEmpty())
                ? List.of()
                : sortedChatRoomIdsSet.stream().map(id -> Long.valueOf(id.toString())).collect(Collectors.toList());

        // 최종 정렬된 채팅방 ID 리스트 생성
        List<Long> finalChatRoomIds = new ArrayList<>();
        sortedChatRoomIds.stream().filter(validChatRoomIds::contains).forEach(finalChatRoomIds::add);
        validChatRoomIds.stream().filter(id -> !finalChatRoomIds.contains(id)).forEach(finalChatRoomIds::add);

        return finalChatRoomIds.stream()
                .map(chatRoomRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }


    //채팅방에 있는 사용자 조회
    public List<User> getUserByRoomId(Long roomId){
        return userChatRoomRepository.findUsersByChatRoomId(roomId);
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
