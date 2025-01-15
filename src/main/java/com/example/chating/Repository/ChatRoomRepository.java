package com.example.chating.Repository;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT ucr.chatRoom FROM UserChatRoom ucr WHERE ucr.user.id = :userId")
    List<ChatRoom> findChatRoomsByUserId(Long userId);
}

