package com.example.chating.domain.chat;

import com.example.chating.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatroom_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.EAGER) // EAGER로 변경
    @JoinColumn(name = "sender_id")
    private User sender;

    private String content;

    @CreationTimestamp
    private LocalDateTime sentAt;

    // 메시지 읽음 상태를 추적할 사용자 리스트
    @ManyToMany
    private Set<User> readByUsers = new HashSet<>();

    public void addReadByUser(User user) {
        this.readByUsers.add(user);
    }
}
