package com.example.demo.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.model.Roles;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secretKeyValue;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long expirationTime;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userid, Roles role) {
        return Jwts.builder()
                .subject(userid)
                .claim("role", role.getRoleName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateToken(Integer userid, Roles role) {
        return generateToken(userid.toString(), role);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getId(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
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
