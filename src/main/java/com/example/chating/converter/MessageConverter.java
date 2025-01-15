package com.example.chating.converter;

import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.UserService;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.chat.Message;

public class MessageConverter {

    private ChatRoomService chatRoomService;
    private UserService userService;

    public Message toEntity(ChatMessage chatMessage) {
        return Message.builder()
                .content(chatMessage.getContent())
                .chatRoom(chatRoomService.getChatRoomById(Long.valueOf(chatMessage.getRoomId())))
                .sender(userService.getUserById(chatMessage.getSender()))
                .sentAt(chatMessage.getSendAt())
                .build();
    }

    public ChatMessage toDto(Message message) {
        return new ChatMessage(
                ChatMessage.MessageType.TALK,               // type
                message.getChatRoom().getId().toString(),   // roomId
                message.getSender().getId(),                // sender
                message.getSender().getName(),
                message.getContent(),                       // content
                message.getSentAt()                         // sendAt
        );
    }


}
