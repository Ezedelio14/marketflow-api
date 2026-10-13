package com.marketflow.api.application.auth;

import com.marketflow.api.application.auth.dto.AuthResponse;
import com.marketflow.api.application.auth.dto.LoginRequest;
import com.marketflow.api.application.auth.dto.RegisterRequest;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.user.User;
import com.marketflow.api.domain.user.UserRepository;
import com.marketflow.api.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final TokenRevocationStore revocationStore;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "E-mail is already registered");
        }
        User user = userRepository.save(User.register(request.email(), passwordEncoder.encode(request.password()), UserRole.CUSTOMER));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid e-mail or password"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is not active");
        }
        return toResponse(user);
    }

    public void logout(String tokenId, Instant expiresAt) {
        revocationStore.revoke(tokenId, expiresAt);
    }

    private AuthResponse toResponse(User user) {
        TokenProvider.IssuedToken token = tokenProvider.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), user.getId(), user.getEmail(), user.getRole());
    }
}
