package com.example.chating.Repository;

import com.example.chating.domain.chat.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatRoomId(Long chatRoomId, Pageable pageable);  // Pageable 사용
    List<Message> findByChatRoomId(Long chatRoomId);  // 전체 조회
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.sentAt DESC")
    List<Message> findLatestMessageByChatRoomId(Long chatRoomId, Pageable pageable);
}
