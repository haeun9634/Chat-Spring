package com.example.chating;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ChatingApplication {

	public static void main(String[] args) {

		SpringApplication.run(ChatingApplication.class, args);
	}

	@Component
	@RequiredArgsConstructor
	public class RedisConnectionTester {
		private final RedisTemplate<String, Object> redisTemplate;

		@EventListener(ApplicationReadyEvent.class)
		public void testConnection() {
			try {
				redisTemplate.opsForValue().set("testKey", "testValue");
				String value = (String) redisTemplate.opsForValue().get("testKey");
				System.out.println("Redis 연결 성공! 저장된 값: " + value);
			} catch (Exception e) {
				System.err.println("Redis 연결 실패: " + e.getMessage());
			}
		}
	}

}
