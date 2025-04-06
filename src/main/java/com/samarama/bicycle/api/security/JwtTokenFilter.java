package com.samarama.bicycle.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = Logger.getLogger(JwtTokenFilter.class.getName());

    private final JwtUtils jwtUtils;

    public JwtTokenFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            logger.info("Processing request to: " + request.getRequestURI());

            if (jwt != null) {
                logger.info("JWT token found");
                if (jwtUtils.validateJwtToken(jwt)) {
                    String email = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("JWT token for user: " + email);

                    // Główna zmiana - pobierz role BEZPOŚREDNIO z tokenu JWT
                    Collection<SimpleGrantedAuthority> authorities;

                    // Sprawdź ścieżkę URL, aby określić kontekst
                    String requestUri = request.getRequestURI();
                    if (requestUri.contains("/api/bicycles")) {
                        // Dla ścieżek związanych z rowerami, przydziel rolę CLIENT jeśli nie ma innych
                        authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"));
                        logger.info("Added ROLE_CLIENT based on request URI: " + requestUri);
                    } else if (requestUri.contains("/api/service")) {
                        // Dla ścieżek związanych z serwisem, przydziel rolę SERVICE jeśli nie ma innych
                        authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"));
                        logger.info("Added ROLE_SERVICE based on request URI: " + requestUri);
                    } else {
                        // W przeciwnym razie próbuj odczytać z tokena
                        authorities = jwtUtils.getRolesFromJwtToken(jwt);
                    }

                    logger.info("Assigned authorities: " + authorities);

                    // Utwórz autentykację bez korzystania z UserDetailsService
                    UserDetails userDetails = new User(email, "", authorities);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("Authentication set for: " + email + " with authorities: " + authorities);
                } else {
                    logger.warning("Invalid JWT token");
                }
            } else {
                logger.info("No JWT token found in request");
            }
        } catch (Exception e) {
            logger.severe("Cannot set user authentication: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}