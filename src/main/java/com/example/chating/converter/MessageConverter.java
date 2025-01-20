package com.example.chating.converter;

import com.example.chating.Dto.MessageDto;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.UserService;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.chat.Message;

import java.time.LocalDateTime;

public class MessageConverter {

    private ChatRoomService chatRoomService;
    private UserService userService;

    public static ChatMessage toChatMessage(MessageDto dto) {
        return ChatMessage.builder()
                .id(null)
                .type(ChatMessage.MessageType.TALK)
                .roomId(dto.getChatRoomId().toString())
                .sender(dto.getSenderId())
                .senderName(dto.getSenderName())
                .content(dto.getContent())
                .sendAt(LocalDateTime.now())
                .build();
    }

    public static MessageDto toMessageDto(ChatMessage message) {
        return MessageDto.builder()
                .chatRoomId(Long.valueOf(message.getRoomId()))
                .senderId(message.getSender())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .build();
    }


}
