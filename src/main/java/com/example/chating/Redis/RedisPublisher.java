package com.example.chating.Redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String topic, Object message) {
        // 로그 추가: 어떤 메시지와 토픽으로 발행하는지 확인
        System.out.println("Publishing message to topic: " + topic + ", message: " + message);
        redisTemplate.convertAndSend(topic, message);
    }
}
