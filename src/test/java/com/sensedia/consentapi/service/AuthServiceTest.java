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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Registro de Usuários")
    class Register {

        @Test
        @DisplayName("Deve registrar um novo usuário com sucesso e retornar tokens")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("luis")
                    .password("senha123")
                    .role(UserRole.ROLE_USER)
                    .build();

            when(userRepository.existsByUsername("luis")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashedPassword");
            when(jwtService.generateAccessToken(any())).thenReturn("access-token-mock");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token-mock");
            when(jwtService.getAccessTokenExpiration()).thenReturn(1800000L);

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("access-token-mock", response.getAccessToken());
            assertEquals("refresh-token-mock", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals("luis", savedUser.getUsername());
            assertEquals("$2a$10$hashedPassword", savedUser.getPassword());
            assertTrue(savedUser.getRoles().contains(UserRole.ROLE_USER));
        }

        @Test
        @DisplayName("Deve usar ROLE_USER como default quando role não é especificado")
        void shouldUseDefaultRoleWhenNotSpecified() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("maria")
                    .password("senha456")
                    .role(null)
                    .build();

            when(userRepository.existsByUsername("maria")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtService.getAccessTokenExpiration()).thenReturn(1800000L);

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertTrue(captor.getValue().getRoles().contains(UserRole.ROLE_USER));
        }

        @Test
        @DisplayName("Deve lançar exceção quando username já existir")
        void shouldThrowWhenUsernameAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("luis")
                    .password("senha123")
                    .build();

            when(userRepository.existsByUsername("luis")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(request));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login de Usuários")
    class Login {

        @Test
        @DisplayName("Deve fazer login com sucesso e retornar tokens")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .username("luis")
                    .password("senha123")
                    .build();

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("luis")
                    .password("hashed")
                    .roles(Set.of(UserRole.ROLE_USER))
                    .build();

            when(userDetailsService.loadUserByUsername("luis")).thenReturn(user);
            when(jwtService.generateAccessToken(user)).thenReturn("access-mock");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh-mock");
            when(jwtService.getAccessTokenExpiration()).thenReturn(1800000L);

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("access-mock", response.getAccessToken());
            assertEquals("refresh-mock", response.getRefreshToken());
            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Deve lançar BadCredentialsException quando senha for inválida")
        void shouldThrowWhenPasswordIsWrong() {
            LoginRequest request = LoginRequest.builder()
                    .username("luis")
                    .password("senha-errada")
                    .build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Refresh de Tokens")
    class Refresh {

        @Test
        @DisplayName("Deve renovar access token com refresh token válido")
        void shouldRefreshAccessToken() {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .build();

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("luis")
                    .password("hashed")
                    .roles(Set.of(UserRole.ROLE_USER))
                    .build();

            when(tokenBlacklistService.isBlacklisted("valid-refresh-token")).thenReturn(false);
            when(jwtService.extractUsername("valid-refresh-token")).thenReturn("luis");
            when(userDetailsService.loadUserByUsername("luis")).thenReturn(user);
            when(jwtService.isTokenValid("valid-refresh-token", user)).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(1800000L);

            AuthResponse response = authService.refresh(request);

            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("valid-refresh-token", response.getRefreshToken());
        }

        @Test
        @DisplayName("Deve lançar exceção quando refresh token for inválido")
        void shouldThrowWhenRefreshTokenIsInvalid() {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("invalid-refresh-token")
                    .build();

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("luis")
                    .password("hashed")
                    .roles(Set.of(UserRole.ROLE_USER))
                    .build();

            when(tokenBlacklistService.isBlacklisted("invalid-refresh-token")).thenReturn(false);
            when(jwtService.extractUsername("invalid-refresh-token")).thenReturn("luis");
            when(userDetailsService.loadUserByUsername("luis")).thenReturn(user);
            when(jwtService.isTokenValid("invalid-refresh-token", user)).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> authService.refresh(request));
        }

        @Test
        @DisplayName("Deve lançar exceção quando refresh token estiver na blacklist")
        void shouldThrowWhenRefreshTokenIsBlacklisted() {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("blacklisted-refresh-token")
                    .build();

            when(tokenBlacklistService.isBlacklisted("blacklisted-refresh-token")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> authService.refresh(request));
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Deve adicionar access token à blacklist no logout")
        void shouldBlacklistAccessTokenOnLogout() {
            String accessToken = "access-token";
            when(jwtService.extractUsername(accessToken)).thenReturn("luis");

            authService.logout(accessToken, null);

            verify(tokenBlacklistService).blacklist(accessToken);
        }

        @Test
        @DisplayName("Deve adicionar access e refresh tokens à blacklist quando refresh é fornecido")
        void shouldBlacklistBothTokensOnLogout() {
            String accessToken = "access-token";
            LogoutRequest request = LogoutRequest.builder()
                    .refreshToken("refresh-token")
                    .build();

            when(jwtService.extractUsername(accessToken)).thenReturn("luis");

            authService.logout(accessToken, request);

            verify(tokenBlacklistService).blacklist(accessToken);
            verify(tokenBlacklistService).blacklist("refresh-token");
        }

        @Test
        @DisplayName("Não deve tentar invalidar refresh token quando não fornecido")
        void shouldNotBlacklistRefreshWhenNotProvided() {
            String accessToken = "access-token";
            LogoutRequest request = LogoutRequest.builder().build();

            when(jwtService.extractUsername(accessToken)).thenReturn("luis");

            authService.logout(accessToken, request);

            verify(tokenBlacklistService, times(1)).blacklist(accessToken);
            verifyNoMoreInteractions(tokenBlacklistService);
        }
    }
}
