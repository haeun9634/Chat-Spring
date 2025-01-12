package com.example.chating.domain.chat.Controller;

import com.example.chating.domain.chat.Dto.LoginRequestDTO;
import com.example.chating.domain.chat.Dto.LoginResponseDTO;
import com.example.chating.domain.chat.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return authService.login(loginRequestDTO);
    }


    @PostMapping("/signup")
    public String signUp(@RequestBody LoginRequestDTO loginRequestDTO) {
        authService.signUp(loginRequestDTO);
        return "User registered successfully";
    }

    @PostMapping("/refresh")
    public LoginResponseDTO refresh(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authService.refreshToken(token);
    }



}