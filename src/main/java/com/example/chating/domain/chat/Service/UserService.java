package com.example.chating.domain.chat.Service;

import com.example.chating.domain.User;
import com.example.chating.domain.chat.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public User findByName(String name) {
        return userRepository.findByName(name).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
