package com.example.chating.global.config;

import java.security.Principal;

public class AuthenticatedUser implements Principal {
    private String username;
    private Long userId;

    // 생성자
    public AuthenticatedUser(String username, Long userId) {
        this.username = username;
        this.userId = userId;
    }

    @Override
    public String getName() {
        return this.username;
    }

    public Long getUserId() {
        return this.userId;
    }
}


