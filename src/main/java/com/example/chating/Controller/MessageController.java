package com.example.chating.Controller;

import com.example.chating.Service.MessageService;
import com.example.chating.global.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final TokenProvider tokenProvider;

    /**
     * 특정 채팅방 내 모든 메시지 읽음 처리
     * - 사용자가 특정 채팅방 내의 모든 메시지를 읽은 상태로 처리
     *
     * @param roomId 채팅방 ID
     * @param token 사용자 인증을 위한 JWT 토큰
     * @return ResponseEntity
     */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAllMessagesAsRead(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token) {

        // JWT 토큰에서 userId 추출
        Long userId = extractUserIdFromToken(token);

        // 채팅방 내 모든 메시지를 읽은 상태로 처리
        messageService.markAllMessagesAsRead(roomId, userId);

        return ResponseEntity.ok().build();
    }

    private Long extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        return tokenProvider.extractClaims(token).get("userId", Long.class); // JWT에서 사용자 ID 추출
    }



}
