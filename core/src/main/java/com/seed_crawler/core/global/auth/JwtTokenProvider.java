package com.seed_crawler.core.global.auth;


import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.entity.enums.MemberRole;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final Key key;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15분
    private long accessTokenExpirationMs;

    @Getter
    @Value("${jwt.refresh-token-expiration-ms:1209600000}") // 14일
    private long refreshTokenExpirationMs;

    private final String AUTH_HEADER = "Authorization";
    private final String TOKEN_PREFIX = "Bearer ";

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTH_HEADER);

        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        return null;
    }

    public AuthDto.TokenResponse generateTokens(UUID memberId, MemberRole role) {
        String accessToken = createToken(memberId, role, accessTokenExpirationMs);
        String refreshToken = createToken(memberId, role, refreshTokenExpirationMs);
        return new AuthDto.TokenResponse(accessToken, refreshToken);
    }

    private String createToken(UUID memberId, MemberRole role, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(memberId.toString())
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID getMemberId(String token) {
        return UUID.fromString(Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
