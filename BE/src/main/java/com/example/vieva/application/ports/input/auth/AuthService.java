package com.example.vieva.application.ports.input.auth;

public interface AuthService {
    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
}
