package br.com.prospectaai.ms_async_task.domain.util;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    @Value("${security.jwt.secret}")
    private String SECRET_KEY;

    public String extractUserId(String authToken) {
        String token = authToken.replace("Bearer ", "");
        if(token.isEmpty()) {
            throw new IllegalArgumentException("Token is empty");
        }
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                .build()
                .parseClaimsJws(token);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid token");
        }

        String userId = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(getSigningKey()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return userId;
    }

    private byte[] getSigningKey() {
        String s = SECRET_KEY != null ? SECRET_KEY.trim() : "";
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
}
