package com.example.chating.domain.chat.Service;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.Dto.LoginRequestDTO;
import com.example.chating.domain.chat.Dto.LoginResponseDTO;
import com.example.chating.domain.chat.Repository.UserRepository;
import com.example.chating.global.TokenProvider;
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
        // 중복 사용자 체크
        if (userRepository.findByName(signUpRequestDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signUpRequestDTO.getPassword());

        // 사용자 저장
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
                String accessToken = tokenProvider.generateAccessToken(user.getName(), user.getId());
                String refreshToken = tokenProvider.generateRefreshToken(user.getName());
                return new LoginResponseDTO(accessToken, refreshToken);
            }
        }
        throw new RuntimeException("Invalid username or password");
    }

    // Refresh Token 검증 및 Access Token 재발급
    public LoginResponseDTO refreshToken(String refreshToken) {
        // Refresh Token 유효성 검증
        if (!tokenProvider.isValidRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Refresh Token에서 사용자 정보 추출
        String username = tokenProvider.extractUsernameFromRefreshToken(refreshToken);

        // 사용자 조회
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 새로운 Access Token 생성
        String newAccessToken = tokenProvider.generateAccessToken(user.getName(), user.getId());

        // 클라이언트가 새 Access Token을 받을 수 있도록 반환
        return new LoginResponseDTO(newAccessToken, null); // Refresh Token은 반환하지 않음
    }
}