package com.example.chating.Controller;

import com.example.chating.Dto.MessageDto;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
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

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final TokenProvider tokenProvider;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload MessageDto messageDto, @Header("Authorization") String token) {
        System.out.println("token: "+token);
        System.out.println("messageDto: "+messageDto);
        // 토큰에서 사용자 ID 추출
        Long senderId = extractUserIdFromToken(token);
        System.out.println("senderId: "+senderId);


        // 메시지 저장
        messageService.saveMessage(roomId, senderId, messageDto.getContent());

        // 최신 활동 시간 업데이트
        chatRoomService.updateChatRoomActivity(roomId);

        // 메시지를 해당 채팅방 구독자들에게 전송
        messageDto.setSenderId(senderId); // 메시지 DTO에 senderId 설정
        messagingTemplate.convertAndSend("/topic/" + roomId, messageDto);

        // 사용자 채팅방 목록 업데이트
        ChatRoom updatedChatRoom = chatRoomService.getChatRoomById(roomId);
        messagingTemplate.convertAndSend("/topic/chatrooms", updatedChatRoom);
    }

    private Long extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        return tokenProvider.extractClaims(token).get("userId", Long.class); // 토큰에서 사용자 ID 추출
    }


}
