package com.thejas.ai_frms.auth.service;

import com.thejas.ai_frms.auth.dto.AuthResponse;
import com.thejas.ai_frms.auth.dto.LoginRequest;
import com.thejas.ai_frms.auth.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}