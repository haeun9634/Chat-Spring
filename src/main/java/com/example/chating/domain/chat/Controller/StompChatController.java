package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.domain.chat.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompChatController {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송 도구

    @MessageMapping("/chat/message") // 클라이언트에서 전송할 목적지
    public void handleChatMessage(ChatMessage chatMessage) {
        System.out.println("Received message: " + chatMessage);

        // 메시지 저장
        Long chatRoomId = Long.parseLong(chatMessage.getRoomId());
        Long senderId = Long.parseLong(chatMessage.getSender());
        messageService.saveMessage(chatRoomId, senderId, chatMessage.getMessage());

        // 동적으로 메시지 브로드캐스트
        String destination = "/topic/chatroom/" + chatMessage.getRoomId();
        messagingTemplate.convertAndSend(destination, chatMessage);
    }
}