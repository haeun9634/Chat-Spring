package com.example.chating.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {
    private Long id;
    private MessageType type;
    private String roomId;
    private Long sender;
    private String senderName; // 사용자 이름 필드 추가
    private String content;
    private LocalDateTime sendAt;
    @JsonProperty("isRead")
    @JsonAlias("read")
    private boolean isRead; // 메시지 읽음 여부
    private int readByUsersCount; // 메시지를 읽은 사용자 수 추가

    public ChatMessage(Long id,MessageType type, String roomId, Long sender, String senderName, String content, LocalDateTime sendAt) {
        this(id, type, roomId, sender, senderName, content, sendAt, false, 0);
    }


    // 기존 생성자 수정
    public ChatMessage(MessageType type, String roomId, Long sender, String senderName, String content, LocalDateTime sendAt) {
        this(null, type, roomId, sender, senderName, content, sendAt, false, 0);
    }

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT
    }
}
