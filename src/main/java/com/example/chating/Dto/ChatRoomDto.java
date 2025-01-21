package com.example.chating.Dto;

import com.example.chating.domain.chat.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ChatRoomDto {
    private ChatRoom chatRoom;       // 채팅방 정보
    private String latestMessage;    // 최신 메시지 내용
    private List<UserProfileDto> userProfiles; // 사용자 프로필 목록
}

