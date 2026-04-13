package com.sensedia.consentapi.service;

import com.sensedia.consentapi.domain.User;
import com.sensedia.consentapi.domain.UserRole;
import com.sensedia.consentapi.dto.AuthResponse;
import com.sensedia.consentapi.dto.LoginRequest;
import com.sensedia.consentapi.dto.LogoutRequest;
import com.sensedia.consentapi.dto.RefreshRequest;
import com.sensedia.consentapi.dto.RegisterRequest;
import com.sensedia.consentapi.repository.UserRepository;
import com.sensedia.consentapi.security.JwtService;
import com.sensedia.consentapi.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username já está em uso: " + request.getUsername());
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_USER;

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
        log.info("Novo usuário registrado: {} com role {}", user.getUsername(), role);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Login bem-sucedido para o usuário: {}", request.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token foi revogado.");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails user = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token inválido ou expirado.");
        }

        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Token renovado para o usuário: {}", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    public void logout(String accessToken, LogoutRequest request) {
        tokenBlacklistService.blacklist(accessToken);

        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            tokenBlacklistService.blacklist(request.getRefreshToken());
        }

        String username = jwtService.extractUsername(accessToken);
        log.info("Logout realizado para o usuário: {}", username);
    }
}
