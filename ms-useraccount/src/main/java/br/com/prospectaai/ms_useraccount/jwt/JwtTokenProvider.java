/*
 * @(#)JwtTokenProvider.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.jwt;

import java.util.Date;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.reset-expiration-ms:900000}")
    private long resetExpirationMs;

    public String generateToken(RegisterResponse res) {
        return Jwts.builder()
                .setSubject(res.getEmail())
                .claim("preRegisterScope", res.getScope().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(UserAccountEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generatePreRegisterValidatedToken(java.util.UUID preRegisterId, String email, PreRegisterScope scope) {
        return Jwts.builder()
                .setSubject(email)
                .claim("preRegisterScope", scope.name())
                .claim("preRegisterValidated", true)
                .claim("preRegisterId", preRegisterId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            System.out.println("[UserAccount][JwtTokenProvider] validating token");
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                .build()
                .parseClaimsJws(token);
            System.out.println("[UserAccount][JwtTokenProvider] token is valid");
            return true;
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            System.out.println("[UserAccount][JwtTokenProvider] token invalid: " + e.getMessage());
            return false;
        }
    }

    private byte[] getSigningKey() {
        String s = secretKey != null ? secretKey.trim() : "";
        boolean maybeBase64 = s.matches("^[A-Za-z0-9+/=]+$") && (s.length() % 4 == 0);
        if (maybeBase64) {
            try {
                byte[] decoded = Decoders.BASE64.decode(s);
                if (decoded != null && decoded.length >= 32) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public String generateResetPasswordToken(String email, java.util.UUID resetId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("purpose", "password_reset")
                .claim("resetId", resetId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + resetExpirationMs))
                .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isResetPasswordToken(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return "password_reset".equals(claims.get("purpose"));
        } catch (JwtException e) {
            return false;
        }
    }
}
