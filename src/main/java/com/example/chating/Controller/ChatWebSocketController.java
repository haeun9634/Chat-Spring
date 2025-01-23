package com.example.chating.Controller;

import com.example.chating.Dto.*;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
import com.example.chating.Service.UserService;
import com.example.chating.converter.MessageConverter;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.global.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final TokenProvider tokenProvider;
    private final UserService userService;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload MessageDto messageDto, @Header("Authorization") String token) {

        // 토큰에서 사용자 ID 추출
        Long senderId = extractUserIdFromToken(token);
        messageDto.setSenderId(senderId);

        // 사용자 이름 조회
        String senderName = userService.getUserNameById(senderId);
        messageDto.setSenderName(senderName);

        // 메시지 저장
        ChatMessage savedMessage = messageService.saveMessage(roomId, senderId, messageDto.getContent());

        // 최신 활동 시간 업데이트
        chatRoomService.updateChatRoomActivity(roomId);

        // 메시지 브로커로 전송 (실시간 채팅)
        messagingTemplate.convertAndSend("/topic/" + roomId, savedMessage);

//        // RedisPublisher를 사용해 메시지 발행
//        redisPublisher.publish("chatroom." + roomId, savedMessage);

        // 사용자 채팅방 목록 업데이트 (ChatRoomDto 생성 후 전송)
        ChatRoom updatedChatRoom = chatRoomService.getChatRoomById(roomId);
        String latestMessage = messageService.getLatestMessageContentFromDb(roomId);
        List<UserProfileDto> userProfiles = chatRoomService.getUserProfilesByChatRoomId(roomId);

        ChatRoomDto chatRoomDto = new ChatRoomDto(updatedChatRoom, latestMessage, userProfiles);
        messagingTemplate.convertAndSend("/topic/chatrooms", chatRoomDto);

        // 로그 출력
        System.out.println("Sending ChatMessage: " + savedMessage);
        System.out.println("Sending ChatRoomDto: " + chatRoomDto);
    }

//    @MessageMapping("/chat/{roomId}/read")
//    public void updateReadStatus(@DestinationVariable Long roomId, @Payload MessageDto messageDto, @Header("Authorization") String token) {
//        // 토큰에서 사용자 ID 추출
//        Long userId = extractUserIdFromToken(token);
//
//        // 읽음 처리
//        messageService.updateReadStatus(roomId, userId);
//
//        // 모든 사용자에게 읽음 상태 브로드캐스트
//        messagingTemplate.convertAndSend("/topic/" + roomId + "/read", new ReadStatusDto(roomId, userId));
//    }

//    @MessageMapping("/chat/{roomId}/active")
//    public void handleActiveStatus(
//            @DestinationVariable Long roomId,
//            @Payload ActiveStatusDto activeStatus,
//            @Header("Authorization") String token) {
//
//        Long userId = extractUserIdFromToken(token);
//        boolean isActive = activeStatus.isActive();
//
//        chatRoomService.updateUserActiveStatus(roomId, userId, isActive);
//
//        System.out.println("User " + userId + " is now " + (isActive ? "active" : "inactive") + " in room " + roomId);
//    }





    private Long extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        return tokenProvider.extractClaims(token).get("userId", Long.class); // 토큰에서 사용자 ID 추출
    }


}
