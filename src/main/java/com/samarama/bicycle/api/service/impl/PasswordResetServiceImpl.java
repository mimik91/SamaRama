package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.PasswordResetDto;
import com.samarama.bicycle.api.dto.PasswordResetRequestDto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.model.VerificationToken;
import com.samarama.bicycle.api.repository.IncompleteUserRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.repository.VerificationTokenRepository;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.PasswordResetService;
import com.samarama.bicycle.api.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = Logger.getLogger(PasswordResetServiceImpl.class.getName());

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final IncompleteUserRepository incompleteUserRepository;
    private final VerificationService verificationService;

    @Autowired
    public PasswordResetServiceImpl(
            UserRepository userRepository,
            VerificationTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder, IncompleteUserRepository incompleteUserRepository, VerificationService verificationService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.incompleteUserRepository = incompleteUserRepository;
        this.verificationService = verificationService;
    }

    @Override
    @Transactional
    public ResponseEntity<?> requestPasswordReset(PasswordResetRequestDto requestDto) {
        String email = requestDto.email();

        // Only registered users (User) can reset passwords
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Check if this email belongs to an IncompleteUser
            boolean incompleteUserExists = incompleteUserRepository.existsByEmail(email);

            if (incompleteUserExists) {
                // This is a guest who ordered without registration
                return ResponseEntity.ok(Map.of(
                        "message", "Ten adres email jest powiązany z zamówieniem jako gość. Aby uzyskać pełny dostęp, zarejestruj się.",
                        "isGuestUser", true
                ));
            } else {
                // Standard "not found" response that doesn't reveal if the user exists
                logger.info("Password reset requested for non-existent user: " + email);
                return ResponseEntity.ok(Map.of("message",
                        "Jeśli konto o podanym adresie email istnieje, instrukcja resetowania hasła została wysłana"));
            }
        }

        User user = userOpt.get();

        // Check verification status
        if (!user.isVerified()) {
            logger.info("Password reset requested for unverified user: " + email);

            // Resend verification email
            VerificationToken token = verificationService.createVerificationToken(user);
            emailService.sendVerificationEmail(user, token.getToken());

            return ResponseEntity.ok(Map.of(
                    "message", "Twoje konto nie zostało jeszcze zweryfikowane. Wysłaliśmy nowy link aktywacyjny na podany adres email. Aktywuj konto, aby móc zresetować hasło.",
                    "requiresVerification", true
            ));
        }

        // Proceed with password reset for verified users
        VerificationToken token = createPasswordResetToken(user);
        emailService.sendPasswordResetEmail(user, token.getToken());

        return ResponseEntity.ok(Map.of("message",
                "Instrukcja resetowania hasła została wysłana na podany adres email"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(PasswordResetDto resetDto) {
        String token = resetDto.token();
        String newPassword = resetDto.newPassword();

        // Znajdź token
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            logger.warning("Password reset token not found: " + token);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Nieprawidłowy token resetowania hasła"));
        }

        VerificationToken verificationToken = tokenOpt.get();

        // Sprawdź czy token nie wygasł i nie był już użyty
        if (verificationToken.isExpired()) {
            logger.warning("Expired password reset token: " + token);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Token resetowania hasła wygasł. Poproś o nowy link."));
        }

        if (verificationToken.isUsed()) {
            logger.warning("Password reset token already used: " + token);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Token resetowania hasła został już użyty. Poproś o nowy link."));
        }

        // Zmień hasło użytkownika
        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Oznacz token jako użyty
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        logger.info("Password reset successfully for user: " + user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Hasło zostało pomyślnie zmienione"));
    }

    private VerificationToken createPasswordResetToken(User user) {
        // Sprawdź, czy istnieje już token dla tego użytkownika
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);

        VerificationToken token;
        if (existingTokenOpt.isPresent()) {
            // Jeśli token już istnieje, aktualizujemy go
            token = existingTokenOpt.get();
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(LocalDateTime.now().plusHours(2)); // Krótszy czas ważności dla bezpieczeństwa
            token.setUsed(false);
        } else {
            // Tworzymy nowy token
            token = new VerificationToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(LocalDateTime.now().plusHours(2));
            token.setUsed(false);
        }

        return tokenRepository.save(token);
    }
}