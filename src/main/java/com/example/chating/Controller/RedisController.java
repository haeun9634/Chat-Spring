package com.example.chating.Controller;

import com.example.chating.Service.RedisService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisController {

    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * Redis 초기화 엔드포인트
     * 예: DELETE /redis/clear
     */
    @DeleteMapping("/redis/clear")
    public String clearRedis() {
        redisService.clearAllData();
        return "Redis 데이터가 초기화되었습니다.";
    }
}
