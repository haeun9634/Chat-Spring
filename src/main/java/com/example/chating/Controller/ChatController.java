package com.example.chating.Controller;

import com.example.chating.Dto.ChatRoomDto;
import com.example.chating.Dto.MessageDto;
import com.example.chating.Repository.UserRepository;
import com.example.chating.domain.MessageType;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.User;
import com.example.chating.Dto.ChatMessage;
import com.example.chating.Service.ChatRoomService;
import com.example.chating.Service.MessageService;
import com.example.chating.global.TokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chat") // API 기본 URL 설정
@RequiredArgsConstructor // final 필드에 대해 생성자를 통한 의존성 자동 주입
public class ChatController {

    private final ChatRoomService chatRoomService; // 채팅방 관리 서비스
    private final MessageService messageService; // 메시지 관리 서비스
    private final TokenProvider tokenProvider; // JWT 토큰 관련 유틸리티
    private final UserRepository userRepository;

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
            @RequestParam String name) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        System.out.println(name);
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
     * @param userId 초대할 사용자 ID
     * @return 추가 결과
     */
    @Operation(summary = "사용자 채팅방 추가", description = "관리자가 사용자를 지정된 채팅방에 추가합니다.")
    @PostMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> addUserToChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        chatRoomService.addUserToChatRoom(roomId, userId); // 채팅방에 사용자 추가
        return ResponseEntity.ok().build(); // 추가 성공 응답 반환
    }


    @Operation(summary = "사용자 채팅방 제거", description = "사용자를 지정된 채팅방에서 제거합니다.")
    @DeleteMapping("/rooms/{roomId}/users")
    public ResponseEntity<Void> removeUserFromChatRoom(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        // 토큰에서 사용자 ID 추출
        Long userId = extractUserIdFromToken(token);

        // 사용자 이름 조회
        String userName = userRepository.getUserNameById(userId);

        // 채팅방에서 사용자 제거
        chatRoomService.removeUserFromChatRoom(roomId, userId);

//        // 퇴장 메시지 WebSocket으로 브로드캐스트
//        handleExitMessage(roomId, userId, userName);

        return ResponseEntity.noContent().build();
    }
//    private final SimpMessagingTemplate messagingTemplate;
//
//    public void handleExitMessage(Long roomId, Long senderId, String senderName) {
//        ChatMessage exitMessage = new ChatMessage(
//                MessageType.EXIT,
//                roomId.toString(),
//                senderId,
//                senderName,
//                senderName + "님이 퇴장하셨습니다.",
//                LocalDateTime.now()
//        );
//        messagingTemplate.convertAndSend("/topic/" + roomId, exitMessage);
//    }



    /**
     * 채팅방 사용자 조회
     * - 특정 채팅방에 있는 사용자들을 조회합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 채팅방 id
     * @return 사용자 조회
     */
    @Operation(summary = "채팅방 사용자 조회 ", description = "특정 채팅방에 있는 사용자들을 조회합니다. ")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<User>> sendMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        List <User> userList = chatRoomService.getUserByRoomId(roomId);

        return ResponseEntity.ok(userList); // 성공 응답 반환
    }

    /**
     * 메시지 조회
     * - 특정 채팅방의 모든 메시지를 조회합니다.
     *
     * @param token Authorization 헤더에 포함된 JWT 토큰
     * @param roomId 메시지 조회 대상 채팅방 ID
     * @return 채팅방에 포함된 메시지 목록
     */
    @Operation(summary = "메시지 조회", description = "지정된 채팅방의 메시지를 페이지네이션을 사용하여 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page, // 페이지 번호 (기본값: 0)
            @RequestParam(defaultValue = "20") int size // 페이지 크기 (기본값: 20)
    ) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        List<ChatMessage> messages = messageService.getMessagesByChatRoomWithReadUpdate(roomId,userId, page, size);
        return ResponseEntity.ok(messages);
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
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token); // JWT 토큰에서 사용자 ID 추출
        List<ChatRoomDto> chatRooms = chatRoomService.getChatRoomsByUser(userId); // 참여 중인 채팅방 조회
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
