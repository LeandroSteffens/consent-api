package com.sensedia.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "dGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIGp3dCBhdXRoZW50aWNhdGlvbg==";
    private static final long ACCESS_TOKEN_EXPIRATION = 1800000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        setField(jwtService, "secretKey", TEST_SECRET);
        setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private UserDetails createTestUser(String username, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new User(username, "password", authorities);
    }

    @Nested
    @DisplayName("Geração de Tokens")
    class TokenGeneration {

        @Test
        @DisplayName("Deve gerar um access token não nulo e não vazio")
        void shouldGenerateAccessToken() {
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String token = jwtService.generateAccessToken(user);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(2, token.chars().filter(c -> c == '.').count());
        }

        @Test
        @DisplayName("Deve gerar um refresh token não nulo e não vazio")
        void shouldGenerateRefreshToken() {
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String token = jwtService.generateRefreshToken(user);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Access e Refresh tokens devem ser diferentes")
        void accessAndRefreshTokensShouldBeDifferent() {
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            assertNotEquals(accessToken, refreshToken);
        }
    }

    @Nested
    @DisplayName("Extração de Claims")
    class ClaimExtraction {

        @Test
        @DisplayName("Deve extrair o username corretamente do access token")
        void shouldExtractUsernameFromAccessToken() {
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String token = jwtService.generateAccessToken(user);

            assertEquals("luis", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Deve extrair o username corretamente do refresh token")
        void shouldExtractUsernameFromRefreshToken() {
            UserDetails user = createTestUser("admin_user", "ROLE_ADMIN");
            String token = jwtService.generateRefreshToken(user);

            assertEquals("admin_user", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Deve incluir os roles como claim no access token")
        void shouldIncludeRolesInAccessToken() {
            UserDetails user = createTestUser("admin", "ROLE_ADMIN", "ROLE_USER");
            String token = jwtService.generateAccessToken(user);

            @SuppressWarnings("unchecked")
            List<String> roles = jwtService.extractClaim(token, claims ->
                    claims.get("roles", List.class));

            assertNotNull(roles);
            assertTrue(roles.contains("ROLE_ADMIN"));
            assertTrue(roles.contains("ROLE_USER"));
        }
    }

    @Nested
    @DisplayName("Validação de Tokens")
    class TokenValidation {

        @Test
        @DisplayName("Deve validar com sucesso um token válido e não expirado")
        void shouldValidateValidToken() {
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String token = jwtService.generateAccessToken(user);

            assertTrue(jwtService.isTokenValid(token, user));
        }

        @Test
        @DisplayName("Deve rejeitar token quando o username não corresponde")
        void shouldRejectTokenWhenUsernameMismatch() {
            UserDetails originalUser = createTestUser("luis", "ROLE_USER");
            UserDetails differentUser = createTestUser("outro_usuario", "ROLE_USER");
            String token = jwtService.generateAccessToken(originalUser);

            assertFalse(jwtService.isTokenValid(token, differentUser));
        }

        @Test
        @DisplayName("Deve rejeitar token expirado lançando ExpiredJwtException")
        void shouldRejectExpiredToken() throws Exception {
            setField(jwtService, "accessTokenExpiration", 0L);
            UserDetails user = createTestUser("luis", "ROLE_USER");
            String token = jwtService.generateAccessToken(user);

            Thread.sleep(10);

            assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar parsear token com assinatura inválida")
        void shouldRejectTokenWithInvalidSignature() {
            String fakeToken = Jwts.builder()
                    .subject("luis")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(Keys.hmacShaKeyFor(
                            Decoders.BASE64.decode("b3V0cmEgY2hhdmUgc2VjcmV0YSBtdWl0byBkaWZlcmVudGUgZGEgb3JpZ2luYWw=")))
                    .compact();

            assertThrows(Exception.class, () -> jwtService.extractUsername(fakeToken));
        }
    }

    @Nested
    @DisplayName("Tempos de Expiração")
    class ExpirationTimes {

        @Test
        @DisplayName("Access token deve ter expiração de 30 minutos")
        void accessTokenShouldHave30MinExpiration() {
            assertEquals(1800000L, jwtService.getAccessTokenExpiration());
        }

        @Test
        @DisplayName("Refresh token deve ter expiração de 7 dias")
        void refreshTokenShouldHave7DayExpiration() {
            assertEquals(604800000L, jwtService.getRefreshTokenExpiration());
        }
    }
}
