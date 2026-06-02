package com.thejas.ai_frms.auth.service;

import com.thejas.ai_frms.auth.dto.AuthResponse;
import com.thejas.ai_frms.auth.dto.LoginRequest;
import com.thejas.ai_frms.auth.dto.RegisterRequest;
import com.thejas.ai_frms.auth.entity.UserEntity;
import com.thejas.ai_frms.auth.repository.UserRepository;
import com.thejas.ai_frms.common.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail    = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BadRequestException("Username is already taken: " + request.getUsername());
        }

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository
                .findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Account is inactive. Please contact support.");
        }

        // UUID token — frontend stores it; no server-side validation needed without Spring Security
        String token = UUID.randomUUID().toString();

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }
}