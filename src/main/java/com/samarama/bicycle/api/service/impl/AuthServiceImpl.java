package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.IncompleteUserRepository;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.security.JwtUtils;
import com.samarama.bicycle.api.service.AuthService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.logging.Logger;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = Logger.getLogger(AuthServiceImpl.class.getName());

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final UserDataMigrationService userDataMigrationService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtUtils jwtUtils,
                           VerificationService verificationService,
                           EmailService emailService, IncompleteUserRepository incompleteUserRepository, IncompleteBikeRepository incompleteBikeRepository, ServiceOrderRepository serviceOrderRepository, UserDataMigrationService userDataMigrationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.verificationService = verificationService;
        this.emailService = emailService;
        this.userDataMigrationService = userDataMigrationService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> authenticateClient(LoginDto loginDto) {
        try {
            // Create authentication token with additional context info
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password());

            // Add context information
            Map<String, String> details = new HashMap<>();
            details.put("loginContext", "client");
            authToken.setDetails(details);

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            User user = userRepository.findByEmail(loginDto.email())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Sprawdź czy konto jest zweryfikowane
            if (!user.isVerified()) {
                return ResponseEntity.status(403).body(Map.of(
                        "message", "Konto nie zostało zweryfikowane. Sprawdź swoją skrzynkę email.",
                        "verified", false,
                        "email", user.getEmail()
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("verified", user.isVerified());
            if (user.hasRole("ROLE_ADMIN")) {
                response.put("role", "ADMIN");
            } else if (user.hasRole("ROLE_MODERATOR")) {
                response.put("role", "MODERATOR");
            } else {
                response.put("role", "CLIENT");
            }

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Authentication error: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> registerClient(UserRegistrationDto registrationDto) {
        String email = registrationDto.email();

        // Sprawdź, czy istnieje już użytkownik z tym adresem email
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email jest już zajęty przez zarejestrowanego użytkownika"));
        }

        // Użyj nowej usługi do utworzenia użytkownika lub konwersji IncompleteUser
        User savedUser = userDataMigrationService.registerAndMigrateData(registrationDto);

        // Wyślij email weryfikacyjny
        VerificationToken verificationToken = verificationService.createVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser, verificationToken.getToken());

        return ResponseEntity.ok(Map.of(
                "message", "Zarejestrowano pomyślnie! Sprawdź swoją skrzynkę email, aby aktywować konto.",
                "email", savedUser.getEmail()
        ));
    }

    @Override
    public ResponseEntity<Map<String, String>> registerService(BikeServiceDto bikeServiceDto) {
        return ResponseEntity.ok(Map.of("message", "Bike service registered successfully!"));
    }
}