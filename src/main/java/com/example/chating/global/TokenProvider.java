package com.example.chating.global;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenProvider {

    private static final SecretKey ACCESS_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Access Token용 비밀키
    private static final SecretKey REFRESH_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Refresh Token용 비밀키

    // Access Token 생성
    public String generateAccessToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1시간 유효
                .signWith(ACCESS_KEY)
                .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 604800000)) // 7일 유효
                .signWith(REFRESH_KEY)
                .compact();
    }

    // Refresh Token 검증
    public boolean isValidRefreshToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(REFRESH_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Refresh Token에서 사용자명 추출
    public String extractUsernameFromRefreshToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(REFRESH_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}