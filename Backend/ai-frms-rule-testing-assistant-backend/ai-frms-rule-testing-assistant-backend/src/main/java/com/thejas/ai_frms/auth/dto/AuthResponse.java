package com.thejas.ai_frms.auth.dto;

public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String username;
    private String fullName;
    private String role;

    public AuthResponse(String token, Long userId, String email, String username, String fullName, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
