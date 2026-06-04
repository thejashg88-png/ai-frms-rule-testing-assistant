package com.thejas.ai_frms.auth.service;

import com.thejas.ai_frms.auth.dto.AuthResponse;
import com.thejas.ai_frms.auth.dto.LoginRequest;
import com.thejas.ai_frms.auth.dto.RegisterRequest;
import com.thejas.ai_frms.auth.entity.UserEntity;
import com.thejas.ai_frms.auth.repository.UserRepository;
import com.thejas.ai_frms.auth.security.JwtTokenProvider;
import com.thejas.ai_frms.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "TESTER", "VIEWER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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
        user.setRole(resolveRole(request.getRole()));

        userRepository.save(user);
        log.info("[AUTH] Registered username={}, role={}", normalizedUsername, user.getRole());
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

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        log.info("[AUTH] username={}, role={}", user.getUsername(), user.getRole());

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    private String resolveRole(String input) {
        if (input == null || input.isBlank()) return "TESTER";
        String upper = input.trim().toUpperCase();
        return VALID_ROLES.contains(upper) ? upper : "TESTER";
    }
}