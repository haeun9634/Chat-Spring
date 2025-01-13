package com.example.chating.Service;

import com.example.chating.domain.User;
import com.example.chating.Dto.LoginRequestDTO;
import com.example.chating.Dto.LoginResponseDTO;
import com.example.chating.Repository.UserRepository;
import com.example.chating.global.TokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenProvider tokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void signUp(LoginRequestDTO signUpRequestDTO) {
        if (userRepository.findByName(signUpRequestDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        String encodedPassword = passwordEncoder.encode(signUpRequestDTO.getPassword());
        User user = new User();
        user.setName(signUpRequestDTO.getUsername());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Optional<User> optionalUser = userRepository.findByName(loginRequestDTO.getUsername());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                String token = tokenProvider.generateToken(user.getName(), user.getId());
                return new LoginResponseDTO(token);
            }
        }
        throw new RuntimeException("Invalid username or password");
    }

    public LoginResponseDTO refreshToken(String token) {
        // 토큰에서 클레임 추출
        Claims claims = tokenProvider.extractClaims(token);

        // 클레임에서 사용자 정보 추출
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        // 사용자 확인
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 새 토큰 발급
        String newToken = tokenProvider.generateToken(user.getName(), user.getId());
        return new LoginResponseDTO(newToken);
    }
}
