package com.example.chating.Service;

import com.example.chating.domain.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
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

//    // 사용자 채팅방 목록
//    public List<ChatRoom> getChatRoomsByUser(Long userId) {
//        String userChatRoomsKey = "user:" + userId + ":chatrooms";
//        Set<Object> chatRoomIds = redisTemplate.opsForSet().members(userChatRoomsKey);
//
//        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
//            System.out.println("No chat rooms found for user: " + userId);
//            return List.of();
//        }
//
//        return chatRoomIds.stream()
//                .map(id -> {
//                    Object chatRoomObj = redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, id.toString());
//                    System.out.println("Fetched ChatRoom from Redis: " + chatRoomObj);
//                    if (chatRoomObj instanceof ChatRoom) {
//                        return (ChatRoom) chatRoomObj;
//                    } else {
//                        System.out.println("Invalid ChatRoom data for ID: " + id);
//                        return null;
//                    }
//                })
//                .filter(chatRoom -> chatRoom != null)
//                .collect(Collectors.toList());
//    }

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

        // 참여 중인 채팅방이 없으면 빈 리스트 반환
        if (userChatRoomIdsSet == null || userChatRoomIdsSet.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        // Set<Object>를 List<String>으로 변환
        List<String> userChatRoomIds = userChatRoomIdsSet.stream()
                .map(Object::toString)  // Object -> String 변환
                .collect(Collectors.toList());

        System.out.println("사용자가 참여한 채팅방 ID: " + userChatRoomIds);

        // 최신 활동 기준으로 정렬된 채팅방 ID 가져오기
        Set<Object> sortedChatRoomIdsSet = redisTemplate.opsForZSet()
                .reverseRangeByScore(CHAT_ROOM_ACTIVITY_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        if (sortedChatRoomIdsSet == null || sortedChatRoomIdsSet.isEmpty()) {
            sortedChatRoomIdsSet = Set.of(); // 빈 Set 반환
        }

        // Set<Object>를 List<String>으로 변환
        List<String> sortedChatRoomIds = sortedChatRoomIdsSet.stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        System.out.println("정렬된 채팅방 ID: " + sortedChatRoomIds);

        // 정렬된 채팅방 ID를 먼저 추가하고 나머지 사용자 채팅방 ID를 추가
        List<String> finalChatRoomIds = new ArrayList<>();

        // 1. 정렬된 채팅방 ID 중 사용자 채팅방 ID와 교집합 부분을 먼저 추가
        sortedChatRoomIds.stream()
                .filter(userChatRoomIds::contains)  // 사용자 채팅방 목록에 있는 정렬된 채팅방 ID만 추가
                .forEach(finalChatRoomIds::add);

        // 2. 나머지 사용자 채팅방 ID 추가 (중복 제거)
        userChatRoomIds.stream()
                .filter(id -> !finalChatRoomIds.contains(id))  // 이미 추가된 ID는 제외
                .forEach(finalChatRoomIds::add);

        System.out.println("최종 정렬된 채팅방 ID: " + finalChatRoomIds);

        // 최종 정렬된 채팅방 ID를 기반으로 채팅방 객체 가져오기
        return finalChatRoomIds.stream()
                .map(id -> redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, id)) // 각 채팅방 ID로부터 채팅방 객체 조회
                .filter(chatRoomObj -> chatRoomObj instanceof ChatRoom)  // ChatRoom 객체만 필터링
                .map(chatRoomObj -> (ChatRoom) chatRoomObj)  // ChatRoom 타입으로 변환
                .collect(Collectors.toList());  // 리스트로 반환
    }



    // 채팅방 ID로 채팅방 정보 조회
    public ChatRoom getChatRoomById(Long chatRoomId) {
        Object chatRoomObj = redisTemplate.opsForHash().get(CHAT_ROOMS_KEY, chatRoomId.toString());
        if (chatRoomObj instanceof ChatRoom) {
            return (ChatRoom) chatRoomObj;
        }
        return null;
    }


}
