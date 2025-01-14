package com.example.chating.Controller;

import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.Dto.MessageDto;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
import com.example.chating.global.TokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat") // API 기본 URL 설정
@RequiredArgsConstructor // final 필드에 대해 생성자를 통한 의존성 자동 주입
public class ChatController {

    private final ChatRoomService chatRoomService; // 채팅방 관리 서비스
    private final MessageService messageService; // 메시지 관리 서비스
    private final TokenProvider tokenProvider; // JWT 토큰 관련 유틸리티

    /**
     * 채팅방 생성
     * - 새로운 채팅방을 생성합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param name 생성할 채팅방의 이름
     * @return 생성된 채팅방 정보
     */
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createChatRoom(
            @RequestHeader("Authorization") String token,
            @RequestBody String name) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        ChatRoom chatRoom = chatRoomService.createChatRoom(name); // 채팅방 생성 로직 호출
        return ResponseEntity.ok(chatRoom); // 생성된 채팅방 반환
    }

    /**
     * 채팅방 삭제
     * - 특정 채팅방을 삭제합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 삭제할 채팅방 ID
     * @return 삭제 결과
     */
    @Operation(summary = "채팅방 삭제", description = "기존 채팅방을 삭제합니다.")
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        chatRoomService.deleteChatRoom(roomId); // 채팅방 삭제 로직 호출
        return ResponseEntity.noContent().build(); // 삭제 성공 응답 반환
    }

    /**
     * 사용자 채팅방 추가
     * - 특정 사용자(userId)를 특정 채팅방(roomId)에 추가합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 사용자 추가 대상 채팅방 ID
     * @return 추가 결과
     */
    @Operation(summary = "사용자 채팅방 추가", description = "사용자를 지정된 채팅방에 추가합니다.")
    @PostMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> addUserToChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        chatRoomService.addUserToChatRoom(roomId, userId); // 채팅방에 사용자 추가
        return ResponseEntity.ok().build(); // 추가 성공 응답 반환
    }

    /**
     * 사용자 채팅방 제거
     * - 특정 사용자(userId)를 특정 채팅방(roomId)에서 제거합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 사용자 제거 대상 채팅방 ID
     * @return 제거 결과
     */
    @Operation(summary = "사용자 채팅방 제거", description = "사용자를 지정된 채팅방에서 제거합니다.")
    @DeleteMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> removeUserFromChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        chatRoomService.removeUserFromChatRoom(roomId, userId); // 채팅방에서 사용자 제거
        return ResponseEntity.noContent().build(); // 제거 성공 응답 반환
    }

    /**
     * 메시지 전송
     * - 특정 채팅방에 메시지를 전송합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param messageDto 메시지 정보 (채팅방 ID, 메시지 내용 등)
     * @return 메시지 전송 결과
     */
//    @Operation(summary = "메시지 전송", description = "지정된 채팅방에 메시지를 전송합니다.")
//    @PostMapping("/messages")
//    public ResponseEntity<Void> sendMessage(
//            @RequestHeader("Authorization") String token,
//            @RequestBody MessageDto messageDto) {
//        Long senderId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
//        messageService.saveMessage(messageDto.getChatRoomId(), senderId, messageDto.getContent()); // 메시지 저장
//        // 최신 활동 시간 업데이트
//        chatRoomService.updateChatRoomActivity(messageDto.getChatRoomId());
//        return ResponseEntity.ok().build(); // 메시지 전송 성공 응답 반환
//    }

    /**
     * 메시지 조회
     * - 특정 채팅방의 모든 메시지를 조회합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 메시지 조회 대상 채팅방 ID
     * @return 채팅방에 포함된 메시지 목록
     */
    @Operation(summary = "메시지 조회", description = "지정된 채팅방의 모든 메시지를 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        List<ChatMessage> messages = messageService.getMessagesByChatRoom(roomId); // 메시지 조회
        return ResponseEntity.ok(messages); // 메시지 목록 반환
    }

    /**
     * 사용자가 참여한 채팅방 조회
     * - 사용자가 현재 참여 중인 채팅방 목록을 조회합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @return 사용자가 참여 중인 채팅방 목록
     */
    @Operation(summary = "사용자 참여 채팅방 조회", description = "사용자가 현재 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping("/users/rooms")
    public ResponseEntity<List<ChatRoom>> getUserChatRooms(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        List<ChatRoom> chatRooms = chatRoomService.getChatRoomsByUser(userId); // 참여 중인 채팅방 조회
        return ResponseEntity.ok(chatRooms); // 채팅방 목록 반환
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * - Authorization 헤더에서 "Bearer "를 제거한 후 JWT를 파싱하여 사용자 ID를 추출합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @return 사용자 ID
     */
    private Long extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        return tokenProvider.extractClaims(token).get("userId", Long.class); // 토큰에서 사용자 ID 추출
    }
}
