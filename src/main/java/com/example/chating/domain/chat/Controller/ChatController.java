package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Dto.MessageDto;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.Service.ChatRoomService;
import com.example.chating.domain.chat.Service.MessageService;
import com.example.chating.global.TokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    private final TokenProvider tokenProvider;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createChatRoom(
            @RequestHeader("Authorization") String token,
            @RequestBody String name) {
        Long userId = extractUserIdFromToken(token);
        return ResponseEntity.ok(chatRoomService.createChatRoom(name));
    }

    @Operation(summary = "채팅방 삭제", description = "기존 채팅방을 삭제합니다.")
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long userId = extractUserIdFromToken(token);
        chatRoomService.deleteChatRoom(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사용자 채팅방 추가", description = "사용자를 지정된 채팅방에 추가합니다.")
    @PostMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> addUserToChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        Long userId = extractUserIdFromToken(token);
        chatRoomService.addUserToChatRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 채팅방 제거", description = "사용자를 지정된 채팅방에서 제거합니다.")
    @DeleteMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> removeUserFromChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        Long userId = extractUserIdFromToken(token);
        chatRoomService.removeUserFromChatRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메시지 전송", description = "지정된 채팅방에 메시지를 전송합니다.")
    @PostMapping("/messages")
    public ResponseEntity<Void> sendMessage(
            @RequestHeader("Authorization") String token,
            @RequestBody MessageDto messageDto) {
        Long senderId = extractUserIdFromToken(token);
        messageService.saveMessage(messageDto.getChatRoomId(), senderId, messageDto.getContent());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "메시지 조회", description = "지정된 채팅방의 모든 메시지를 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId,
            Pageable pageable) {
        extractUserIdFromToken(token); // 인증만 수행, 반환값은 사용하지 않음
        return ResponseEntity.ok(messageService.getMessagesByChatRoom(roomId, pageable));
    }

    @Operation(summary = "사용자가 참여한 채팅방 조회", description = "사용자가 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping("/users/rooms")
    public ResponseEntity<List<ChatRoom>> getUserChatRooms(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        System.out.println(userId);
        return ResponseEntity.ok(chatRoomService.getChatRoomsByUser(userId));
    }

    // JWT 토큰에서 사용자 ID 추출
    private Long extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }
        return tokenProvider.extractClaims(token).get("userId", Long.class);
    }
}
