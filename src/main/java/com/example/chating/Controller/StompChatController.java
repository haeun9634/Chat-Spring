package com.example.chating.Controller;

//@Controller
//@RequiredArgsConstructor
//public class StompChatController {
//    private final MessageService messageService;
//    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송 도구
//
//    @MessageMapping("/chat/message") // 클라이언트에서 전송할 목적지
//    public void handleChatMessage(ChatMessage chatMessage) {
//        System.out.println("Received message: " + chatMessage);
//
//        // 메시지 저장
//        Long chatRoomId = Long.parseLong(chatMessage.getRoomId());
//        Long senderId = Long.parseLong(chatMessage.getSender());
//        messageService.saveMessage(chatRoomId, senderId, chatMessage.getMessage());
//
//        // 동적으로 메시지 브로드캐스트
//        String destination = "/topic/chatroom/" + chatMessage.getRoomId();
//        messagingTemplate.convertAndSend(destination, chatMessage);
//    }
//}