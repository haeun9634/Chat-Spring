package com.example.chating.Controller;

import com.example.chating.Dto.*;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
import com.example.chating.Service.UserService;
import com.example.chating.converter.MessageConverter;
import com.example.chating.domain.MessageType;
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

import java.time.LocalDateTime;
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

//        // 메시지 저장
//        ChatMessage savedMessage = messageService.saveMessage(roomId, senderId, messageDto.getContent(), messageDto.getMessageType());

        // 최신 활동 시간 업데이트
        chatRoomService.updateChatRoomActivity(roomId);

        MessageType messageType = messageDto.getMessageType();

        switch (messageType) {
            case ENTER: // 사용자가 채팅방에 입장한 경우
                handleEnterMessage(roomId, messageDto.getContent());
                break;

            case TALK: // 일반 채팅 메시지
                handleTalkMessage(roomId, senderId, messageDto);
                break;

            case EXIT: // 사용자가 채팅방에서 나간 경우
                handleExitMessage(roomId, senderId, senderName);
                break;

            case MATCH_REQUEST: // 매칭 요청 메시지
                handleMatchRequestMessage(roomId, senderId, senderName);
                break;

            case MATCH: // 매칭 완료 메시지
                handleMatchMessage(roomId);
                break;

            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }

//        // 로그 출력
//        System.out.println("Sending ChatMessage: " + savedMessage);
//        System.out.println("Sending ChatRoomDto: " + chatRoomDto);
    }

    private void handleEnterMessage(Long roomId, String content) {
        Long inviteId;
        try {
            inviteId = Long.parseLong(content);
        } catch (NumberFormatException e) {
            System.err.println("Invalid inviteId format: " + content);
            return;
        }

        String inviteName = userService.getUserNameById(inviteId);
        if (inviteName == null || inviteName.isEmpty()) {
            System.err.println("Invitee name not found for ID: " + inviteId);
            return;
        }

        ChatMessage enterMessage = new ChatMessage(
                MessageType.ENTER,
                roomId.toString(),
                inviteId,
                inviteName,
                inviteName + "님이 입장하셨습니다.",
                LocalDateTime.now()
        );

        // 메시지 저장 및 브로드캐스트
        messageService.saveMessage(roomId, inviteId, enterMessage.getContent(), MessageType.ENTER);
        messagingTemplate.convertAndSend("/topic/" + roomId, enterMessage);

        System.out.println("Broadcasted enter message: " + enterMessage);
    }


    private void handleTalkMessage(Long roomId, Long senderId, MessageDto messageDto) {

        // 메시지 저장
        ChatMessage savedMessage = messageService.saveMessage(roomId, senderId, messageDto.getContent(), messageDto.getMessageType());


        // 메시지 브로커로 전송 (실시간 채팅)
        messagingTemplate.convertAndSend("/topic/" + roomId, savedMessage);

//        // RedisPublisher를 사용해 메시지 발행
//        redisPublisher.publish("chatroom." + roomId, savedMessage);

        // 사용자 채팅방 목록 업데이트 (ChatRoomDto 생성 후 전송)
        ChatRoom updatedChatRoom = chatRoomService.getChatRoomById(roomId);
        String latestMessage = messageService.getLatestMessageContent(roomId);
        List<UserProfileDto> userProfiles = chatRoomService.getUserProfilesByChatRoomId(roomId);

        ChatRoomDto chatRoomDto = new ChatRoomDto(updatedChatRoom, latestMessage, userProfiles);
        messagingTemplate.convertAndSend("/topic/chatrooms", chatRoomDto);

    }

    public void handleExitMessage(Long roomId, Long senderId, String senderName) {
        ChatMessage exitMessage = new ChatMessage(
                MessageType.EXIT,
                roomId.toString(),
                senderId,
                senderName,
                senderName + "님이 퇴장하셨습니다.",
                LocalDateTime.now()
        );
        messageService.saveMessage(roomId, senderId,exitMessage.getContent(), MessageType.EXIT);
        messagingTemplate.convertAndSend("/topic/" + roomId, exitMessage);
    }

    private void handleMatchRequestMessage(Long roomId, Long senderId, String senderName) {
        ChatMessage matchRequestMessage = new ChatMessage(
                MessageType.MATCH_REQUEST,
                roomId.toString(),
                senderId,
                senderName,
                senderName + "님이 매칭을 요청하였습니다.",
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/" + roomId, matchRequestMessage);
    }
    private void handleMatchMessage(Long roomId) {
        ChatMessage matchMessage = new ChatMessage(
                MessageType.MATCH,
                roomId.toString(),
                null, // 시스템 메시지로 처리
                "System",
                "매칭이 완료되었습니다!",
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/" + roomId, matchMessage);
    }

    @MessageMapping("/chat/{roomId}/read")
    public void updateReadStatus(
            @DestinationVariable Long roomId,
            @Payload Map<String, Object> payload,
            @Header("Authorization") String token
    ) {
        Long userId = extractUserIdFromToken(token);
        messageService.updateReadStatus(roomId, userId);

        // 읽음 상태를 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/" + roomId + "/read",
                new ReadStatusDto(roomId, userId, chatRoomService.getReadByUsersCount(roomId))
        );
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
