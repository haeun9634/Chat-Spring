package com.example.chating.Controller;

import com.example.chating.Dto.MessageDto;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
import com.example.chating.domain.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload MessageDto messageDto) {
        // 메시지 저장
        messageService.saveMessage(roomId, messageDto.getSenderId(), messageDto.getContent());

        // 최신 활동 시간 업데이트
        chatRoomService.updateChatRoomActivity(roomId);

        // 메시지를 해당 채팅방 구독자들에게 전송
        messagingTemplate.convertAndSend("/topic/" + roomId, messageDto);

        // 사용자 채팅방 목록 업데이트 (최신 메시지가 도착한 채팅방을 맨 위로)
        ChatRoom updatedChatRoom = chatRoomService.getChatRoomById(roomId);
        messagingTemplate.convertAndSend("/topic/chatrooms", updatedChatRoom);
    }
}
