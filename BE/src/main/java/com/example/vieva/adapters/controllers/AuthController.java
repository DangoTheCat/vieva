package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.AuthPresenter;
import com.example.vieva.adapters.presenters.AuthResponse;
import com.example.vieva.adapters.controllers.request.LoginApiRequest;
import com.example.vieva.adapters.controllers.request.RegisterApiRequest;
import com.example.vieva.application.ports.input.LoginRequest;
import com.example.vieva.application.ports.input.RegisterRequest;
import com.example.vieva.application.ports.output.AuthResult;
import com.example.vieva.application.usecases.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthPresenter authPresenter;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterApiRequest apiRequest) {
        RegisterRequest request = RegisterRequest.builder()
                .email(apiRequest.getEmail())
                .password(apiRequest.getPassword())
                .fullName(apiRequest.getFullName())
                .phoneNumber(apiRequest.getPhoneNumber())
                .build();

        AuthResult result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(authPresenter.toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginApiRequest apiRequest) {
        LoginRequest request = LoginRequest.builder()
                .email(apiRequest.getEmail())
                .password(apiRequest.getPassword())
                .build();

        AuthResult result = authService.login(request);
        return ResponseEntity.ok(authPresenter.toResponse(result));
    }
}
