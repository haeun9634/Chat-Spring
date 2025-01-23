package com.example.chating.domain.chat;

import com.example.chating.domain.MessageType;
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

    private boolean isRead = false; // 메시지 읽음 상태 필드 추가

    @Enumerated(EnumType.STRING) // ENUM 타입으로 저장
    private MessageType messageType;

    // 읽은 사용자 추가
    public void addReadByUser(User user) {
        this.readByUsers.add(user);
    }

    // setIsRead 메서드 추가
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    // setReadByUsersCount 메서드 추가
    public void setReadByUsersCount(int readByUsersCount) {
        // 이 메서드는 readByUsersCount 값을 설정합니다.
        // 이 메서드를 통해 외부에서 읽은 사용자 수를 업데이트할 수 있습니다.
        this.readByUsersCount = readByUsersCount;
    }

    private int readByUsersCount; // 읽은 사용자 수를 추적하는 필드 추가
}
