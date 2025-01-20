package com.example.chating.Controller;

import com.example.chating.Dto.ChatMessage;
import com.example.chating.Dto.MessageDto;
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
        String senderName = userService.getUserNameById(senderId); // UserService에서 사용자 이름 가져오기
        messageDto.setSenderName(senderName);

        // MessageDto를 ChatMessage로 변환
        ChatMessage chatMessage = MessageConverter.toChatMessage(messageDto);

        // 메시지 저장 (여기서 DB에 저장되며 ID가 생성됨)
        ChatMessage savedMessage = messageService.saveMessage(roomId, senderId, chatMessage.getContent());

        // 생성된 ID를 ChatMessage에 설정
        chatMessage.setId(savedMessage.getId());

        // 최신 활동 시간 업데이트
        chatRoomService.updateChatRoomActivity(roomId);

        // 메시지 브로커로 전송
        messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);

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
