package com.sensedia.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklist(String token) {
        Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();

        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "revoked",
                    ttlMillis,
                    TimeUnit.MILLISECONDS
            );
            log.info("Token adicionado à blacklist com TTL de {}ms", ttlMillis);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
