package com.sensedia.auth.controller;

import com.sensedia.auth.dto.AuthResponse;
import com.sensedia.auth.dto.LoginRequest;
import com.sensedia.auth.dto.LogoutRequest;
import com.sensedia.auth.dto.RefreshRequest;
import com.sensedia.auth.dto.RegisterRequest;
import com.sensedia.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro, login, logout e renovação de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar novo usuário",
            description = "Cria um novo usuário e retorna os tokens JWT de acesso")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login",
            description = "Autentica o usuário e retorna access + refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout",
            description = "Invalida o access token atual e opcionalmente o refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String accessToken = authHeader.substring(7);
        authService.logout(accessToken, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Renovar token",
            description = "Usa o refresh token para obter um novo access token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
