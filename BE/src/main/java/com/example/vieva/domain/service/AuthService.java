package com.example.vieva.domain.service;

import com.example.vieva.application.dto.AuthResponse;
import com.example.vieva.application.dto.LoginRequest;
import com.example.vieva.application.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
