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
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/bike-services").permitAll()
                                .requestMatchers("/api/bike-services/{id}").permitAll()
                                .requestMatchers("/api/map/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                .requestMatchers("/test").permitAll()
                                .requestMatchers("/api/**").permitAll()
                                .requestMatchers("/api/bicycles/**").permitAll()
                                .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}