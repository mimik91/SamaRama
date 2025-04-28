package com.samarama.bicycle.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@PropertySource("classpath:application.properties")
public class WebSecurityConfig {

    private final JwtAuthEntryPoint unauthorizedHandler;
    private final JwtTokenFilter jwtTokenFilter;

    public WebSecurityConfig(JwtAuthEntryPoint unauthorizedHandler,
                             JwtTokenFilter jwtTokenFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // In WebSecurityConfig.java
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/signin/**").permitAll()
                                .requestMatchers("/api/auth/signup/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                // Dodajemy dostęp do endpointów weryfikacji
                                .requestMatchers("/api/verification/**").permitAll()
                                // Allow access to guest orders endpoint
                                .requestMatchers("/api/guest-orders/**").permitAll()
                                // Add this line to permit access to the service packages endpoint
                                .requestMatchers("/api/service-packages/active").permitAll()
                                // For standard access to bikes (GET)
                                .requestMatchers("/api/bicycles").permitAll()
                                .requestMatchers("/api/bicycles/*/photo").permitAll()
                                .requestMatchers("/api/enumerations/**").permitAll()
                                .requestMatchers("/api/service-orders/package-price/**").permitAll()
                                .requestMatchers("/api/service-slots/availability/**").permitAll()
                                .requestMatchers("/api/service-slots/check-availability").permitAll()
                                .requestMatchers("/api/account/**").permitAll()
                                .requestMatchers("/test").permitAll()
                                .requestMatchers("/api/account/public/**").permitAll()
                                .requestMatchers("/api/account/**").authenticated()
                                // Admin routes require ADMIN or MODERATOR role
                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                                // Only authorized users for modifying bikes
                                .requestMatchers("/api/bicycles/*/photo").authenticated()
                                // Remaining API should be protected
                                .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}