package com.example.chating.domain.chat.Service;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Repository.ChatRoomRepository;
import com.example.chating.domain.chat.Repository.UserChatRoomRepository;
import com.example.chating.domain.chat.UserChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;

    public ChatRoom createChatRoom(String name) {
        ChatRoom chatRoom = ChatRoom.builder().name(name).build();
        return chatRoomRepository.save(chatRoom);
    }

    public void deleteChatRoom(Long chatRoomId) {
        chatRoomRepository.deleteById(chatRoomId);
    }

    public void addUserToChatRoom(Long chatRoomId, Long userId) {
        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(ChatRoom.builder().id(chatRoomId).build())
                .user(User.builder().id(userId).build())
                .build();
        userChatRoomRepository.save(userChatRoom);
    }

    public void removeUserFromChatRoom(Long chatRoomId, Long userId) {
        List<UserChatRoom> userChatRooms = userChatRoomRepository.findByChatRoomId(chatRoomId);
        userChatRooms.stream()
                .filter(userChatRoom -> userChatRoom.getUser().getId().equals(userId))
                .forEach(userChatRoomRepository::delete);
    }

    public ChatRoom findById(Long id) {
        return chatRoomRepository.findById(id).orElseThrow(() -> new RuntimeException("ChatRoom not found"));
    }


    public List<ChatRoom> getChatRoomsByUser(Long userId) {
        return chatRoomRepository.findChatRoomsByUserId(userId);
    }
}
