package com.example.chating.domain.chat;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private MessageType type;
    private String roomId;
    private Long sender;
    private String senderName; // 사용자 이름 필드 추가
    private String content;
    private LocalDateTime sendAt;

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT
    }
}
