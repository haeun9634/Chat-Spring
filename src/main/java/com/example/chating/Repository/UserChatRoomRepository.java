package com.example.chating.Repository;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {
    List<UserChatRoom> findByChatRoomId(Long chatRoomId);
    List<UserChatRoom> findByUserId(Long userId);
    Optional<UserChatRoom> findByUserAndChatRoom(User user, ChatRoom chatRoom);
}

