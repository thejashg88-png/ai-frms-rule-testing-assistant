package com.thejas.ai_frms.auth.security;

import com.thejas.ai_frms.auth.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final UserEntity user;

    public UserDetailsImpl(UserEntity user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = (user.getRole() != null) ? user.getRole().toUpperCase() : "TESTER";
        // Normalise legacy "USER" role to TESTER
        if ("USER".equals(role)) role = "TESTER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return "ACTIVE".equalsIgnoreCase(user.getStatus()); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return "ACTIVE".equalsIgnoreCase(user.getStatus()); }

    public UserEntity getUser() { return user; }
}