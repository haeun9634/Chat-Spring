package com.example.chating.Dto;

import com.example.chating.domain.chat.Message;
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
    private Long senderId;
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

    // Message 객체에서 ChatMessage로 변환하는 생성자 추가
    public ChatMessage(Message message) {
        this.id = message.getId();
        this.type = MessageType.TALK; // 기본적으로 TALK 타입으로 설정
        this.roomId = message.getChatRoom().getId().toString();
        this.senderId = message.getSender().getId();
        this.senderName = message.getSender().getName();
        this.content = message.getContent();
        this.sendAt = message.getSentAt();
        this.isRead = message.isRead();
        this.readByUsersCount = message.getReadByUsersCount();
    }


    // 기존 생성자 수정
    public ChatMessage(MessageType type, String roomId, Long sender, String senderName, String content, LocalDateTime sendAt) {
        this(null, type, roomId, sender, senderName, content, sendAt, false, 0);
    }

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT
    }
}
