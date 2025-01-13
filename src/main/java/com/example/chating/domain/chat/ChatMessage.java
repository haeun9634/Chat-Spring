package com.example.chating.domain.chat;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT;
    }

    private MessageType type;
    private String roomId;
    private Long sender; // Long 타입으로 수정
    private String content; // 필드 이름을 content로 변경
    private LocalDateTime sendAt;
}
