package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.model.VerificationToken;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = Logger.getLogger(AuthServiceImpl.class.getName());

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final VerificationService verificationService;
    private final EmailService emailService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder encoder,
                           JwtUtils jwtUtils,
                           VerificationService verificationService,
                           EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.verificationService = verificationService;
        this.emailService = emailService;
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

    @Override
    @Transactional
    public ResponseEntity<Map<String, String>> registerClient(UserRegistrationDto registrationDto) {
        // Sprawdź czy użytkownik o podanym emailu już istnieje
        if (userRepository.existsByEmail(registrationDto.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email jest już zajęty"));
        }

        User user = new User();
        user.setEmail(registrationDto.email());
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());
        user.setPhoneNumber(registrationDto.phoneNumber());
        user.setPassword(encoder.encode(registrationDto.password()));
        user.addRole("ROLE_CLIENT"); // Ensure role is set
        user.setVerified(false); // Domyślnie konto jest niezweryfikowane
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        try {
            // Tworzenie tokenu weryfikacyjnego
            VerificationToken verificationToken = verificationService.createVerificationToken(savedUser);

            // Wysyłanie maila z linkiem aktywacyjnym
            emailService.sendVerificationEmail(savedUser, verificationToken.getToken());

            logger.info("Verification email sent to: " + savedUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Zarejestrowano pomyślnie! Sprawdź swoją skrzynkę email, aby aktywować konto.",
                    "email", savedUser.getEmail()
            ));
        } catch (Exception e) {
            logger.severe("Error during registration process: " + e.getMessage());
            e.printStackTrace();

            // Pomimo błędu z emailem, użytkownik został zarejestrowany
            return ResponseEntity.ok(Map.of(
                    "message", "Zarejestrowano pomyślnie, ale wystąpił problem z wysyłaniem emaila aktywacyjnego. Skontaktuj się z administracją.",
                    "email", savedUser.getEmail()
            ));
        }
    }

    @Override
    public ResponseEntity<Map<String, String>> registerService(BikeServiceDto bikeServiceDto) {
        return ResponseEntity.ok(Map.of("message", "Bike service registered successfully!"));
    }
}