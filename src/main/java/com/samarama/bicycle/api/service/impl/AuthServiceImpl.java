package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.ServiceUserDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
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
    private final ServiceUserRepository serviceUserRepository;
    private final JwtUtils jwtUtils;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final UserDataMigrationService userDataMigrationService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository, ServiceUserRepository serviceUserRepository,
                           JwtUtils jwtUtils,
                           VerificationService verificationService,
                           EmailService emailService, IndividualUserRepository individualUserRepository, IncompleteBikeRepository incompleteBikeRepository, ServiceOrderRepository serviceOrderRepository, UserDataMigrationService userDataMigrationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.serviceUserRepository = serviceUserRepository;
        this.jwtUtils = jwtUtils;
        this.verificationService = verificationService;
        this.emailService = emailService;
        this.userDataMigrationService = userDataMigrationService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> authenticateClient(LoginDto loginDto) {
        try {
            // Wspólne kroki autentykacji
            String jwt = performAuthentication(loginDto);

            // Sprawdź najpierw zwykłego użytkownika
            Optional<User> userOptional = userRepository.findByEmail(loginDto.email());
            if (userOptional.isPresent()) {
                return authenticateUser(userOptional.get(), jwt);
            }

            // Jeśli nie znaleziono zwykłego użytkownika, sprawdź ServiceUser
            Optional<ServiceUser> serviceUserOptional = serviceUserRepository.findByEmail(loginDto.email());
            if (serviceUserOptional.isPresent()) {
                return authenticateServiceUser(serviceUserOptional.get(), jwt);
            }

            throw new UsernameNotFoundException("User not found");

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
    @Transactional
    public ResponseEntity<Map<String, String>> registerServiceUser(ServiceUserDto serviceUserDto) {
        String email = serviceUserDto.email();

        // Sprawdź, czy istnieje już użytkownik z tym adresem email
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email jest już zajęty przez zarejestrowanego użytkownika"));
        }

        if (serviceUserRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email jest już zajęty przez innego użytkownika serwisu"));
        }

        try {
            // Użyj UserDataMigrationService dla potencjalnej migracji danych z IncompleteUser
            ServiceUser serviceUser = userDataMigrationService.registerServiceUserAndMigrateData(serviceUserDto);

            // Wspólny kod dla wysłania email weryfikacyjnego
            VerificationToken verificationToken = verificationService.createVerificationToken(serviceUser);
            emailService.sendVerificationEmail(serviceUser, verificationToken.getToken());

            return ResponseEntity.ok(Map.of(
                    "message", "Zarejestrowano pomyślnie! Sprawdź swoją skrzynkę email, aby aktywować konto.",
                    "email", serviceUser.getEmail()
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas rejestracji użytkownika serwisu: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Wystąpił błąd podczas rejestracji"));
        }
    }



    private String performAuthentication(LoginDto loginDto) {
        // Create authentication token with additional context info
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password());

        // Add context information
        Map<String, String> details = new HashMap<>();
        details.put("loginContext", "client");
        authToken.setDetails(details);

        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return jwtUtils.generateJwtToken(authentication);
    }

    private ResponseEntity<Map<String, Object>> authenticateUser(User user, String jwt) {
        // Sprawdź weryfikację
        ResponseEntity<Map<String, Object>> verificationResponse = checkVerificationAndRespond(user, "CLIENT");
        if (verificationResponse != null) {
            return verificationResponse;
        }

        // Buduj odpowiedź dla User
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("verified", user.isVerified());
        response.put("userType", "CLIENT");

        if (user.hasRole("ROLE_ADMIN")) {
            response.put("role", "ADMIN");
        } else if (user.hasRole("ROLE_MODERATOR")) {
            response.put("role", "MODERATOR");
        } else {
            response.put("role", "CLIENT");
        }

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> authenticateServiceUser(ServiceUser serviceUser, String jwt) {
        // Sprawdź weryfikację
        ResponseEntity<Map<String, Object>> verificationResponse = checkVerificationAndRespond(serviceUser, "SERVICE");
        if (verificationResponse != null) {
            return verificationResponse;
        }

        // Buduj odpowiedź dla ServiceUser
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", serviceUser.getId());
        response.put("email", serviceUser.getEmail());
        response.put("bikeServiceId", serviceUser.getBikeServiceId());
        response.put("verified", serviceUser.isVerified());
        response.put("userType", "SERVICE");
        response.put("role", "SERVICE");

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> checkVerificationAndRespond(User user, String userType) {


        if (!user.isVerified()) {
            String message = userType.equals("SERVICE")
                    ? "Konto serwisu nie zostało zweryfikowane. Sprawdź swoją skrzynkę email."
                    : "Konto nie zostało zweryfikowane. Sprawdź swoją skrzynkę email.";

            IndividualUser individualUser1 = (IndividualUser) user;
            return ResponseEntity.status(403).body(Map.of(
                    "message", message,
                    "verified", false,
                    "email", individualUser1.getEmail()
            ));
        }
        return null;
    }
}