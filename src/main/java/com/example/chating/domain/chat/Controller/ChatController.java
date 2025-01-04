package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.MessageDto;
import com.example.chating.domain.chat.Service.ChatRoomService;
import com.example.chating.domain.chat.Service.MessageService;
import com.example.chating.global.WebSocketChatHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createChatRoom(@RequestBody String name) {
        return ResponseEntity.ok(chatRoomService.createChatRoom(name));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        chatRoomService.deleteChatRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{roomId}/users/{userId}")
    public ResponseEntity<Void> addUserToChatRoom(@PathVariable Long roomId, @PathVariable Long userId) {
        chatRoomService.addUserToChatRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{roomId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromChatRoom(@PathVariable Long roomId, @PathVariable Long userId) {
        chatRoomService.removeUserFromChatRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages")
    public ResponseEntity<Void> sendMessage(@RequestBody MessageDto messageDto) {
        messageService.saveMessage(messageDto.getChatRoomId(), messageDto.getSenderId(), messageDto.getContent());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(messageService.getMessagesByChatRoom(roomId));
    }
    private WebSocketChatHandler webSocketChatHandler;

    @PostMapping("/rooms/{roomId}/connect")
    public ResponseEntity<Void> connectToChatRoom(@PathVariable Long roomId, WebSocketSession session) {
        webSocketChatHandler.joinChatRoom(roomId, session);
        return ResponseEntity.ok().build();
    }

}
