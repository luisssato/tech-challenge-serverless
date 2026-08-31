package com.fiap.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    /** Construtor de produção: lê JWT_SECRET do ambiente SAM. */
    public JwtUtil() {
        this(System.getenv("JWT_SECRET"), 3600);
    }

    public JwtUtil(String secret, long accessTokenExpirationSeconds) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("JWT_SECRET nao pode ser nulo ou vazio.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve ter pelo menos 32 caracteres.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationSeconds * 1000;
    }

    /**
     * Gera token JWT com subject=cpf e claim role. Compatível com
     * JwtAuthenticationFilter da app principal.
     */
    public String generateAccessToken(String cpf, String role) {
        return Jwts.builder()
                .subject(cpf)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }
}
