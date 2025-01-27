package com.example.chating.Service;

import com.example.chating.Repository.ChatRoomRepository;
import com.example.chating.Repository.UserChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserChatRoomRepository userChatRoomRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * Redis 데이터 초기화 (모든 키 삭제)
     */
    public void clearAllData() {
        try {
            // 모든 키 조회
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                // 키 삭제
                redisTemplate.delete(keys);
                System.out.println("Redis 데이터 초기화 완료. 삭제된 키 개수: " + keys.size());
            } else {
                System.out.println("Redis에 삭제할 키가 없습니다.");
            }
        } catch (Exception e) {
            System.err.println("Redis 초기화 중 오류 발생:");
            e.printStackTrace();
        }
        //db 데이터도 함께 초기화
        userChatRoomRepository.deleteAll();
        chatRoomRepository.deleteAll();
    }
}
