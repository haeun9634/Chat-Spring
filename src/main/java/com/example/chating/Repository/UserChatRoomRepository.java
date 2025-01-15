package com.example.chating.Repository;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {
    List<UserChatRoom> findByChatRoomId(Long chatRoomId);
    List<UserChatRoom> findByUserId(Long userId);
    Optional<UserChatRoom> findByUserAndChatRoom(User user, ChatRoom chatRoom);
    @Query("SELECT uc.user FROM UserChatRoom uc WHERE uc.chatRoom.id = :chatRoomId")
    List<User> findUsersByChatRoomId(@Param("chatRoomId") Long chatRoomId);

}

