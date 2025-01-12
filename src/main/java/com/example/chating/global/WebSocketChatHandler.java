package com.example.chating.global;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.domain.chat.ChatRoom;
import com.example.chating.domain.chat.Message;
import com.example.chating.domain.chat.Service.ChatRoomService;
import com.example.chating.domain.chat.Service.MessageService;
import com.example.chating.domain.chat.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@AllArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper; //JSON 데이터 JAVA로 변환
    private final MessageService messageService;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    private static final Map<Long, List<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
    //각 채팅방의 활성 WebSocket 세션 목록을 관리하는 ConcurrentHashMap
//    public WebSocketChatHandler(ObjectMapper objectMapper, MessageService messageService) {
//        this.objectMapper = objectMapper;
//        this.messageService = messageService;
//    }

    // WebSocket 메시지를 처리할 때
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        // 채팅방과 사용자 조회 (예시로 간단한 코드)
        ChatRoom chatRoom = chatRoomService.findById(Long.parseLong(chatMessage.getRoomId()));
        User sender = userService.findByName(chatMessage.getSender());

        // ChatMessage를 Message 엔티티로 변환
        Message messageEntity = Message.builder()
                .chatRoom(chatRoom)  // 채팅방 정보 설정
                .sender(sender)  // 메시지 보낸 사용자 정보 설정
                .content(chatMessage.getMessage())  // 메시지 내용 설정
                .build();

        // DB에 메시지 저장
        Message savedMessage = messageService.saveMessage(chatRoom.getId(), sender.getId(), chatMessage.getMessage());

        // 읽음 상태 전송
        if ("read".equals(chatMessage.getType())) {
            broadcastReadStatus(savedMessage);  // Message 엔티티의 id를 사용
        }

        // 기존 메시지 처리 로직
        Long chatRoomId = Long.parseLong(chatMessage.getRoomId());
        List<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions != null) {
            for (WebSocketSession webSocketSession : sessions) {
                if (webSocketSession.isOpen()) {
                    // DB에 저장된 메시지를 다른 클라이언트에게 전송
                    webSocketSession.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    // 읽음 상태 전송 로직에서 savedMessage의 id 사용
    private void broadcastReadStatus(Message message) {
        List<WebSocketSession> sessions = chatRoomSessions.get(message.getChatRoom().getId());
        if (sessions != null) {
            for (WebSocketSession webSocketSession : sessions) {
                if (webSocketSession.isOpen()) {
                    try {
                        webSocketSession.sendMessage(new TextMessage("{\"type\": \"read\", \"messageId\": " + message.getId() + "}"));
                    } catch (IOException e) {
                        // 예외 로깅
                        System.err.println("Failed to send read status message: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션 연결 시 특정 로직 추가 가능
    }

    @Override//연결 종료
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRoomSessions.values().forEach(sessions -> sessions.remove(session));
        //저장된 세션 목록에서 종료된 세션 제거
    }

    public void joinChatRoom(Long chatRoomId, WebSocketSession session) {
        chatRoomSessions.computeIfAbsent(chatRoomId, id -> new CopyOnWriteArrayList<>()).add(session);
        //특정 채팅방에 WebSocket 세션 추가. 주어진 chatRoomId에 대응하는 세션 리스트가 존재하지 않을 경우 생성 후 추가
    }
}