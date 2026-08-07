package com.novanest.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.novanest.model.JwtToken;
import com.novanest.model.User;
import com.novanest.repository.JwtTokenRepository;
import com.novanest.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    private final JwtTokenRepository jwtTokenRepository;
    private final UserRepository userRepository;

    public JwtService(JwtTokenRepository jwtTokenRepository, UserRepository userRepository) {
        this.jwtTokenRepository = jwtTokenRepository;
        this.userRepository = userRepository;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver) {

        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> claims,
                                UserDetails userDetails) {

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token,
                                UserDetails userDetails) {

        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(
                        java.util.Base64.getEncoder()
                                .encodeToString(secretKey.getBytes()));

        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Transactional
    public void saveUserToken(String token, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Date expiration = extractExpiration(token);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());

        JwtToken jwtToken = new JwtToken(user, token, expiresAt);
        jwtTokenRepository.save(jwtToken);
    }

    public boolean isTokenStoredInDb(String token) {
        return jwtTokenRepository.findByToken(token)
                .map(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void revokeToken(String token) {
        jwtTokenRepository.findByToken(token).ifPresent(jwtTokenRepository::delete);
    }

    public org.springframework.http.ResponseCookie createJwtCookie(String token) {
        return org.springframework.http.ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtExpiration / 1000)
                .sameSite("Lax")
                .build();
    }

    public org.springframework.http.ResponseCookie createCleanJwtCookie() {
        return org.springframework.http.ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

}