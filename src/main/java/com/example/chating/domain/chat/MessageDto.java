package com.example.chating.domain.chat;

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
}
