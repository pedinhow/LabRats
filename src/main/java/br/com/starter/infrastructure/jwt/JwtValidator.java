package br.com.starter.infrastructure.jwt;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtValidator {

    private final String secretKey;

    public JwtValidator(@Value("${api.security.token.secret}") String secretKey) {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("Missing required configuration: api.security.token.secret");
        }
        this.secretKey = secretKey;
    }

    public Claims validateTokenAndGetClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token expirado");
        } catch (JwtException e) {
            throw new JwtException("Token inválido");
        }
    }

    public boolean isTokenValid(String token, String username) {
        Claims claims = validateTokenAndGetClaims(token);
        String tokenUsername = claims.getSubject();
        return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(claims);
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
