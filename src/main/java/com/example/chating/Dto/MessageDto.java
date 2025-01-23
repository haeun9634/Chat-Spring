package com.example.chating.Dto;

import com.example.chating.domain.MessageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private String senderName;
    private MessageType messageType;

    @Override
    public String toString() {
        return "MessageDto{" +
                "chatRoomId=" + chatRoomId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", senderName='" + senderName + '\'' +
                '}';
    }
}
