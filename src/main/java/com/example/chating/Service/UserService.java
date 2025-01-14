package com.example.chating.Service;

import com.example.chating.domain.User;
import com.example.chating.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User findByName(String name) {
        return userRepository.findByName(name).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    // 사용자 이름 조회
    public String getUserNameById(Long userId) {
        User user = getUserById(userId);
        return user.getName(); // 사용자 이름 반환
    }
}
