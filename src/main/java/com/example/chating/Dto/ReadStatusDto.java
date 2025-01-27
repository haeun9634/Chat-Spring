package com.example.chating.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadStatusDto {
    private Long chatRoomId;
    private Long userId;
    private int readByUsersCount;

    // 기존 생성자
    public ReadStatusDto(Long roomId, Long userId) {
        this.chatRoomId = roomId;
        this.userId = userId;
    }
}

