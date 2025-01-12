package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.domain.chat.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompChatController {
    private final MessageService messageService;

    @MessageMapping("/chat/message") // 클라이언트에서 전송할 목적지
    @SendTo("/topic/chatroom/{roomId}") // 채팅방 구독자들에게 메시지 브로드캐스트
    public ChatMessage handleChatMessage(ChatMessage chatMessage) {
        System.out.println("Received message: {}"+ chatMessage);
        // 메시지 저장
        Long chatRoomId = Long.parseLong(chatMessage.getRoomId());
        Long senderId = Long.parseLong(chatMessage.getSender());
        messageService.saveMessage(chatRoomId, senderId, chatMessage.getMessage());

        // 메시지 반환 (구독자들에게 브로드캐스트)
        return chatMessage;
    }
}
