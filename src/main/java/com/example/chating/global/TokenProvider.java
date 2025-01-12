package com.example.chating.global;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TokenProvider {

    private static final String SECRET_KEY = "AB#4EQS#@@";  // JWT 서명에 사용할 비밀키

    // JWT 생성 메서드 (로그인 시 사용 예시)
    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))  // 1시간 만료
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    // JWT 토큰 유효성 검사
    public boolean isValidToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String jwtToken = token.substring(7);  // "Bearer "를 제거하고 JWT 부분만 추출
                Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(jwtToken);  // 서명 검증
                return true;
            } catch (Exception e) {
                return false;  // 서명 검증 실패 시
            }
        }
        return false;
    }

    // JWT에서 사용자명 추출
    public String extractUsernameFromToken(String token) {
        String jwtToken = token.substring(7);
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwtToken)
                .getBody();
        return claims.getSubject();  // 사용자명을 가져옴 (subject로 저장된 값)
    }

    // JWT에서 사용자 ID 추출
    public Long extractUserIdFromToken(String token) {
        String jwtToken = token.substring(7);
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwtToken)
                .getBody();
        return claims.get("userId", Long.class);  // userId를 claims에서 추출
    }
}
