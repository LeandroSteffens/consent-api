package com.sensedia.consentapi.security;

import com.sensedia.consentapi.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtService jwtService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Deve adicionar token à blacklist com TTL correto")
    void shouldBlacklistTokenWithCorrectTtl() {
        String token = "valid.jwt.token";
        long futureTime = System.currentTimeMillis() + 60000;

        when(jwtService.extractClaim(eq(token), any(Function.class)))
                .thenReturn(new Date(futureTime));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist(token);

        verify(valueOperations).set(
                eq("blacklist:" + token),
                eq("revoked"),
                longThat(ttl -> ttl > 0 && ttl <= 60000),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Não deve adicionar token já expirado à blacklist")
    void shouldNotBlacklistExpiredToken() {
        String token = "expired.jwt.token";
        long pastTime = System.currentTimeMillis() - 60000;

        when(jwtService.extractClaim(eq(token), any(Function.class)))
                .thenReturn(new Date(pastTime));

        tokenBlacklistService.blacklist(token);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Deve retornar true quando token está na blacklist")
    void shouldReturnTrueWhenBlacklisted() {
        String token = "blacklisted.jwt.token";
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(true);

        assertTrue(tokenBlacklistService.isBlacklisted(token));
    }

    @Test
    @DisplayName("Deve retornar false quando token não está na blacklist")
    void shouldReturnFalseWhenNotBlacklisted() {
        String token = "valid.jwt.token";
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(false);

        assertFalse(tokenBlacklistService.isBlacklisted(token));
    }
}
