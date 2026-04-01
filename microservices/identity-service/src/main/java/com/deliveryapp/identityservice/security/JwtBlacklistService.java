package com.deliveryapp.identityservice.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final StringRedisTemplate redisTemplate;

    // Prefijo para identificar fácil los tokens en Redis
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Guarda el token en Redis con un tiempo de expiración (Time To Live).
     * @param token El Access Token que el usuario quiere invalidar.
     * @param expirationTimeInMillis Cuánto tiempo le quedaba de vida natural a ese token.
     */
    public void addToBlacklist(String token, long expirationTimeInMillis) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "revoked",
                expirationTimeInMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Verifica si el token está en la lista negra.
     * Esto lo usará nuestro futuro Filtro de Seguridad.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}