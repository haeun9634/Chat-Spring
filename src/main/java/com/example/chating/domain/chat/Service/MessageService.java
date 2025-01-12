package com.example.chating.domain.chat.Service;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Dto.MessageDto;
import com.example.chating.domain.chat.Repository.MessageRepository;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public Message saveMessage(Long chatRoomId, Long senderId, String content) {
        Message message = Message.builder()
                .chatRoom(ChatRoom.builder().id(chatRoomId).build())
                .sender(User.builder().id(senderId).build())
                .content(content)
                .build();
        return messageRepository.save(message);  // 저장 후 저장된 메시지를 반환
    }



    public List<MessageDto> getMessagesByChatRoom(Long chatRoomId, Pageable pageable) {
        return messageRepository.findByChatRoomId(chatRoomId, pageable).stream()
                .map(message -> new MessageDto(
                        chatRoomId,                      // chatRoomId
                        message.getSender().getId(),     // senderId
                        message.getContent(),            // content
                        message.getSender().getName()    // senderName
                ))
                .collect(Collectors.toList());
    }

    public void markMessageAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
        message.addReadByUser(user);
        messageRepository.save(message);
    }

}

