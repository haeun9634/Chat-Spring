package com.example.chating.Dto;

import com.example.chating.domain.Emoji;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private Long id;          // 사용자 ID
    private String name;      // 사용자 이름
    private Emoji emoji;
}

