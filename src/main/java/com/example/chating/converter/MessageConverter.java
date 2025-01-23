package com.example.chating.converter;

import com.example.chating.Dto.MessageDto;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.UserService;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;

import java.time.LocalDateTime;

public class MessageConverter {

    private ChatRoomService chatRoomService;
    private UserService userService;

    public static ChatMessage toChatMessage(MessageDto dto) {
        return ChatMessage.builder()
                .type(ChatMessage.MessageType.TALK)
                .roomId(dto.getChatRoomId().toString())
                .senderId(dto.getSenderId())
                .senderName(dto.getSenderName())
                .content(dto.getContent())
                .sendAt(LocalDateTime.now())
                .build();
    }

    public static MessageDto toMessageDto(ChatMessage message) {
        return MessageDto.builder()
                .chatRoomId(Long.valueOf(message.getRoomId()))
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .build();
    }

    // ChatMessage -> Message 변환
    public static Message convertChatMessageToMessage(ChatMessage chatMessage, ChatRoom chatRoom, User sender) {
        Message message = new Message();
        message.setChatRoom(chatRoom); // 채팅방 설정
        message.setSender(sender); // 보낸 사용자 설정
        message.setContent(chatMessage.getContent()); // 메시지 내용 설정
        message.setSentAt(LocalDateTime.now()); // 메시지 보낸 시간 설정
        message.setIsRead(false); // 기본적으로 읽음 상태는 false
        message.setReadByUsersCount(0); // 읽은 사용자 수 초기화
        return message;
    }


}
