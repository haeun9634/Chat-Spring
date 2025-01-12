package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.Dto.MessageDto;
import com.example.chating.domain.chat.Service.ChatRoomService;
import com.example.chating.domain.chat.Service.MessageService;
import com.example.chating.global.WebSocketChatHandler;
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
    private final WebSocketChatHandler webSocketChatHandler;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 생성 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatRoom.class)))
    })
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createChatRoom(@RequestBody String name) {
        return ResponseEntity.ok(chatRoomService.createChatRoom(name));
    }

    @Operation(summary = "채팅방 삭제", description = "기존 채팅방을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "채팅방 삭제 성공")
    })
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        chatRoomService.deleteChatRoom(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사용자 채팅방 추가", description = "사용자를 지정된 채팅방에 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 채팅방 추가 성공")
    })
    @PostMapping("/rooms/{roomId}/users/{userId}")
    public ResponseEntity<Void> addUserToChatRoom(@PathVariable Long roomId, @PathVariable Long userId) {
        chatRoomService.addUserToChatRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 채팅방 제거", description = "사용자를 지정된 채팅방에서 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "사용자 채팅방 제거 성공")
    })
    @DeleteMapping("/rooms/{roomId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromChatRoom(@PathVariable Long roomId, @PathVariable Long userId) {
        chatRoomService.removeUserFromChatRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메시지 전송", description = "지정된 채팅방에 메시지를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 전송 성공")
    })
    @PostMapping("/messages")
    public ResponseEntity<Void> sendMessage(@RequestBody MessageDto messageDto) {
        messageService.saveMessage(messageDto.getChatRoomId(), messageDto.getSenderId(), messageDto.getContent());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "메시지 조회", description = "지정된 채팅방의 모든 메시지를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Message.class)))
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long roomId, Pageable pageable) {
        return ResponseEntity.ok(messageService.getMessagesByChatRoom(roomId, pageable));
    }

    @Operation(summary = "채팅방 연결", description = "웹소켓을 통해 채팅방에 연결합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 연결 성공")
    })
    @PostMapping("/rooms/{roomId}/connect")
    public ResponseEntity<Void> connectToChatRoom(@PathVariable Long roomId, WebSocketSession session) {
        webSocketChatHandler.joinChatRoom(roomId, session);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자가 참여한 채팅방 조회", description = "사용자가 참여 중인 채팅방 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여 채팅방 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatRoom.class)))
    })
    @GetMapping("/users/{userId}/rooms")
    public ResponseEntity<List<ChatRoom>> getUserChatRooms(@PathVariable Long userId) {
        List<ChatRoom> chatRooms = chatRoomService.getChatRoomsByUser(userId);
        return ResponseEntity.ok(chatRooms);
    }

}
