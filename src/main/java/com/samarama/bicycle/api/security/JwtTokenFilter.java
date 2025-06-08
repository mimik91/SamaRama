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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = Logger.getLogger(JwtTokenFilter.class.getName());

    // Lista publicznych endpointów, które mają być pominięte przez filtr
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/api/auth/signin/**",
            "/api/auth/signup/**",
            "/api/test/**",
            "/api/verification/**",
            "/api/password/reset-request",
            "/api/password/reset",
            "/api/guest-orders/**",
            "/api/service-packages/active",
            "/api/enumerations/**",  // Dodajemy tu
            "/api/service-orders/package-price/**",
            "/api/service-slots/availability/**",  // Dodajemy tu
            "/api/service-slots/check-availability",
            "/api/guest-orders/**"
    );

    private final JwtUtils jwtUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtTokenFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Pomijamy filtr dla żądań OPTIONS (preflightowych CORS)
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        // Pomijamy filtr dla publicznych endpointów
        return PUBLIC_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            logger.info("Processing request to: " + request.getRequestURI() + " with method: " + request.getMethod());

            if (jwt != null) {
                logger.info("JWT token found");
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String email = jwtUtils.getUserNameFromJwtToken(jwt);
                        logger.info("JWT token for user: " + email);

                        // Get authorities directly from JWT token
                        Collection<SimpleGrantedAuthority> authorities = jwtUtils.getRolesFromJwtToken(jwt);
                        logger.info("JWT token roles: " + authorities);

                        if (authorities.isEmpty()) {
                            logger.warning("No roles found for user: " + email);
                        } else {
                            // Check for CLIENT role
                            boolean hasClientRole = authorities.stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
                            logger.info("User has CLIENT role: " + hasClientRole);
                        }

                        // Special handling for admin panel
                        String requestUri = request.getRequestURI();
                        if (requestUri.contains("/api/admin")) {
                            // Check if user has ADMIN or MODERATOR role
                            boolean hasAdminRole = authorities.stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));

                            if (!hasAdminRole) {
                                logger.warning("Unauthorized access attempt to admin API: " + email);
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                                return;
                            }
                        }

                        // Create authentication
                        UserDetails userDetails = new User(email, "", authorities);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.info("Authentication set for: " + email + " with authorities: " + authorities);
                    } else {
                        logger.warning("Invalid JWT token");
                    }
                } catch (Exception e) {
                    logger.severe("JWT token validation error: " + e.getMessage());
                    e.printStackTrace();
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