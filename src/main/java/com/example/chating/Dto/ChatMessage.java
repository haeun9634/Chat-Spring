package com.example.chating.Dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private MessageType type;
    private String roomId;
    private Long sender;
    private String senderName; // 사용자 이름 필드 추가
    private String content;
    private LocalDateTime sendAt;
    private boolean isRead; // 메시지 읽음 여부
    private int readByUsersCount; // 메시지를 읽은 사용자 수 추가

    // 새로 추가한 생성자 (읽은 사용자 수 포함)
    public ChatMessage(MessageType type, String roomId, Long sender, String senderName, String content, LocalDateTime sendAt, int readByUsersCount, boolean isRead) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.senderName = senderName;
        this.content = content;
        this.sendAt = sendAt;
        this.readByUsersCount = readByUsersCount;
        this.isRead = isRead;
    }

    // 기존 생성자 수정
    public ChatMessage(MessageType type, String roomId, Long sender, String senderName, String content, LocalDateTime sendAt) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.senderName = senderName;
        this.content = content;
        this.sendAt = sendAt;
        this.isRead = false;  // 기본값을 false로 설정
        this.readByUsersCount = 0;  // 기본값 0으로 설정
    }

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT
    }

    public void setReadByUsersCount(int readByUsersCount) {
        this.readByUsersCount = readByUsersCount;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
}
