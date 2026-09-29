package com.example.vieva.application.usecases.auth;

import com.example.vieva.application.ports.input.LoginRequest;
import com.example.vieva.application.ports.input.RegisterRequest;
import com.example.vieva.application.ports.output.AuthResult;

public interface AuthService {
    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
}
