package com.example.chating.domain.chat;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ChatMessage {

    public enum MessageType {
        ENTER, TALK, MATCH_REQUEST, EXIT, MATCH, CHAT;
    }
    private MessageType type;
    private String roomId;
    private String sender;
    private String message;
}
