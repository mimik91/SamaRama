package com.samarama.bicycle.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthEntryPoint unauthorizedHandler;
    private final JwtTokenFilter jwtTokenFilter;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public WebSecurityConfig(JwtAuthEntryPoint unauthorizedHandler,
                             JwtTokenFilter jwtTokenFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/signin/**",
                                "/api/auth/signup/**",
                                "/api/test/**",
                                "/api/verification/**",
                                "/api/password/reset-request",
                                "/api/password/reset",
                                "/api/password/**",
                                "/api/guest-orders/**",
                                "/api/service-packages/active",
                                "/api/enumerations/**",
                                "/api/service-orders/package-price/**",
                                "/api/service-slots/availability/**",
                                "/api/service-slots/config",
                                "/api/service-slots/check-availability",
                                "/api/account/public/**",
                                "/bicycles/bike"
                        ).permitAll()
                        // Endpoints for bike photos
                        .requestMatchers("/api/bicycles/*/photo").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/admin/**", "/api/admin/orders/**").hasAnyRole("ADMIN", "MODERATOR")
                        // Authenticated endpoints
                        .requestMatchers("/api/account/**").authenticated()
                        // Default policy
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Auth-Token"));
        configuration.setExposedHeaders(Arrays.asList("X-Auth-Token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}