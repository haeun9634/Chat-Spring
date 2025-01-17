//package com.example.chating.global;
//
//import com.example.chating.Dto.ChatMessage;
//import com.example.chating.Redis.RedisPublisher;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class WebSocketChatHandler extends TextWebSocketHandler {
//    private final RedisPublisher redisPublisher;
//    private final ObjectMapper objectMapper;
//
//    // 채팅방 ID별 WebSocket 세션을 관리하는 Map
//    private final Map<Long, List<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
//        String payload = message.getPayload();
//        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
//
//        // Redis로 메시지 발행
//        redisPublisher.publish("chatroom." + chatMessage.getRoomId(), chatMessage);
//    }
//
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        // 세션 연결 시 채팅방 ID를 파싱하여 WebSocket 세션 추가
//        Long chatRoomId = getChatRoomIdFromSession(session); // 세션에서 채팅방 ID 추출
//        chatRoomSessions.computeIfAbsent(chatRoomId, id -> new CopyOnWriteArrayList<>()).add(session);
//        System.out.println("Session added to chatRoomId: " + chatRoomId);
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        // 세션 연결 종료 시 해당 채팅방에서 세션 제거
//        chatRoomSessions.values().forEach(sessions -> sessions.remove(session));
//        System.out.println("Session removed: " + session.getId());
//    }
//
//    private Long getChatRoomIdFromSession(WebSocketSession session) {
//        // WebSocketSession에서 채팅방 ID를 추출하는 로직을 구현
//        // 예를 들어, URL 쿼리 파라미터 또는 세션의 특정 속성을 활용
//        String uri = session.getUri().toString();
//        String roomId = uri.substring(uri.lastIndexOf("/") + 1); // 예: "/chat/123"에서 123 추출
//        return Long.parseLong(roomId);
//    }
//}
