package com.example.chating.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadStatusDto {
    private Long chatRoomId;
    private Long userId;
}

