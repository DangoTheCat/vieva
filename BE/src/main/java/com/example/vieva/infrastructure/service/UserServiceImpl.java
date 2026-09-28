package com.example.vieva.infrastructure.service;

import com.example.vieva.domain.entity.User;
import com.example.vieva.domain.repository.UserRepository;
import com.example.vieva.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getByUserCode(String userCode) {
        return userRepository.findByUserCode(userCode);
    }
}
