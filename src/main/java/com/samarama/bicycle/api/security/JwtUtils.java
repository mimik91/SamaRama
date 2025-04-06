package com.samarama.bicycle.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    private static final Logger logger = Logger.getLogger(JwtUtils.class.getName());

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // Zbierz role użytkownika z autentykacji
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Jeśli nie ma ról, sprawdź jaką metodą się zalogował
        if (roles.isEmpty()) {
            // Sprawdź kontekst logowania (jeśli loguje się przez /signin/client, dodaj ROLE_CLIENT)
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                Object loginContext = ((Map<?, ?>) details).get("loginContext");
                if ("client".equals(loginContext)) {
                    roles.add("ROLE_CLIENT");
                } else if ("service".equals(loginContext)) {
                    roles.add("ROLE_SERVICE");
                }
            }
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)  // Zamiast setClaims
                .subject(userPrincipal.getUsername())  // Zamiast setSubject
                .issuedAt(new Date())  // Zamiast setIssuedAt
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))  // Zamiast setExpiration
                .signWith(key())  // Zamiast signWith z dwoma parametrami
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key())  // Zamiast setSigningKey
                .build()
                .parseSignedClaims(token)  // Zamiast parseClaimsJws
                .getPayload()  // Zamiast getBody
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> getRolesFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key())  // Zamiast setSigningKey
                .build()
                .parseSignedClaims(token)  // Zamiast parseClaimsJws
                .getPayload();  // Zamiast getBody

        List<String> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            // Jeśli nie ma ról w tokenie, sprawdź czy możemy określić rolę z kontekstu
            String context = claims.get("context", String.class);
            if ("client".equals(context)) {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"));
            } else if ("service".equals(context)) {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"));
            }
            return Collections.emptyList();
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key())  // Zamiast setSigningKey
                    .build()
                    .parseSignedClaims(authToken);  // Zamiast parseClaimsJws
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warning("Invalid JWT: " + e.getMessage());
        }
        return false;
    }
}