package com.example.chating.Dto;

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
}
