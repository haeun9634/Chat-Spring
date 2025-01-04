package com.example.chating.global;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.ChatMessage;
import com.example.chating.domain.chat.Service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    private static final Map<Long, List<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

    public WebSocketChatHandler(ObjectMapper objectMapper, MessageService messageService) {
        this.objectMapper = objectMapper;
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션 연결 시 특정 로직 추가 가능
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRoomSessions.values().forEach(sessions -> sessions.remove(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        // String으로 전달된 chatRoomId와 sender를 Long 또는 User 객체로 변환
        Long chatRoomId = Long.parseLong(chatMessage.getRoomId());
        User sender = User.builder().id(Long.parseLong(chatMessage.getSender())).build();

        //메시지저장
        messageService.saveMessage(chatRoomId, sender.getId(), chatMessage.getMessage());


        // 메시지 브로드캐스트
        List<WebSocketSession> sessions = chatRoomSessions.get(chatMessage.getRoomId());
        if (sessions != null) {
            for (WebSocketSession webSocketSession : sessions) {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    public void joinChatRoom(Long chatRoomId, WebSocketSession session) {
        chatRoomSessions.computeIfAbsent(chatRoomId, id -> new CopyOnWriteArrayList<>()).add(session);
    }
}