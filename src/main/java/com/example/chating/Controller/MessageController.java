package com.example.chating.Controller;

import com.example.chating.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId, @RequestBody Long userId) {
        messageService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }


}
