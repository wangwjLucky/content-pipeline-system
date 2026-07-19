package com.pipeline.admin.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expiration = 86400000L;

    public JwtUtil(@Value("${jwt.secret:pipeline-secret-key-change-in-prod}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("⚠ JWT 密钥长度不足 32 字节（当前 {} 字节），HS256 可能失败！请使用 openssl rand -hex 32 生成强密钥。", keyBytes.length);
        } else if (keyBytes.length < 48) {
            log.info("JWT 密钥长度 {} 字节，满足 HS256 要求", keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @PostConstruct
    public void init() {
        log.info("JWT 工具初始化完成，Token 有效期 = {} 小时", expiration / 3600000);
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}