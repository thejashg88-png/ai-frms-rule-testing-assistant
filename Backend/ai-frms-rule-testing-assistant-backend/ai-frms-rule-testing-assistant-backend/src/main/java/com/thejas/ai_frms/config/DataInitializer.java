package com.thejas.ai_frms.config;

import com.thejas.ai_frms.auth.entity.UserEntity;
import com.thejas.ai_frms.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds three built-in test users on startup if they don't exist.
 * Credentials: admin/admin123, tester/tester123, viewer/viewer123.
 * Safe to run repeatedly — skips users that already exist.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin",  "admin@frms.local",  "Admin User",  "admin123",  "ADMIN");
        createIfAbsent("tester", "tester@frms.local", "Tester User", "tester123", "TESTER");
        createIfAbsent("viewer", "viewer@frms.local", "Viewer User", "viewer123", "VIEWER");
    }

    private void createIfAbsent(String username, String email, String fullName, String password, String role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus("ACTIVE");
        userRepository.save(user);
        log.info("[INIT] Seed user created: username={}, role={}", username, role);
    }
}