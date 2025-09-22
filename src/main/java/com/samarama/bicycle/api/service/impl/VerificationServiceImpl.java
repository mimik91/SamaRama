package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.model.VerificationToken;
import com.samarama.bicycle.api.repository.IndividualUserRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.repository.VerificationTokenRepository;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.VerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger logger = Logger.getLogger(VerificationServiceImpl.class.getName());

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final IndividualUserRepository individualUserRepository;

    @Value("${app.verification.token.expiration:86400000}")
    private long tokenExpirationMs;

    public VerificationServiceImpl(
            VerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService, IndividualUserRepository individualUserRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.individualUserRepository = individualUserRepository;
    }

    @Transactional
    public VerificationToken createVerificationToken(User user) {
        // Najpierw sprawdź, czy istnieje już token dla tego użytkownika
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);

        VerificationToken verificationToken;
        if (existingTokenOpt.isPresent()) {
            // Jeśli token już istnieje, aktualizuj go
            verificationToken = existingTokenOpt.get();
            verificationToken.setToken(UUID.randomUUID().toString());
            // Ustawienie daty wygaśnięcia (WAŻNE!)
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            verificationToken.setUsed(false);
        } else {
            // Tworzenie nowego tokenu
            verificationToken = new VerificationToken();
            verificationToken.setUser(user);
            verificationToken.setToken(UUID.randomUUID().toString());
            // Ustawienie daty wygaśnięcia (WAŻNE!)
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            verificationToken.setUsed(false);
        }

        return tokenRepository.save(verificationToken);
    }



    @Override
    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyAccount(String token) {
        Optional<VerificationToken> verificationTokenOpt = tokenRepository.findByToken(token);

        if (verificationTokenOpt.isEmpty()) {
            logger.warning("Verification token not found: " + token);
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy token weryfikacyjny"));
        }

        VerificationToken verificationToken = verificationTokenOpt.get();

        // Jeśli token został już wykorzystany, sprawdź czy konto jest już zweryfikowane
        if (verificationToken.isUsed()) {
            User user = verificationToken.getUser();
            if (user.isVerified()) {
                // Jeśli konto jest już zweryfikowane, zwróć komunikat o tym
                return ResponseEntity.ok(Map.of("message", "Konto zostało już wcześniej zweryfikowane. Możesz się zalogować."));
            } else {
                // Jeśli token jest już użyty, ale konto nie jest zweryfikowane (teoretycznie niemożliwe, ale dla pewności),
                // oznaczamy token jako niewykorzystany i kontynuujemy weryfikację
                verificationToken.setUsed(false);
            }
        }

        if (verificationToken.isExpired()) {
            logger.warning("Token expired: " + token);
            return ResponseEntity.badRequest().body(Map.of("message", "Token weryfikacyjny wygasł. Możesz poprosić o wysłanie nowego."));
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        // Wyślij mail z potwierdzeniem aktywacji konta
        emailService.sendAccountActivatedEmail(user);

        logger.info("Account verified successfully for user: " + user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Konto zostało pomyślnie zweryfikowane"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> resendVerificationToken(String email) {
        // Only registered users (User) can have verification tokens
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Check if this email belongs to an IncompleteUser
            boolean incompleteUserExists = individualUserRepository.existsByEmail(email);

            if (incompleteUserExists) {
                // This is a guest who ordered without registration
                return ResponseEntity.ok(Map.of(
                        "message", "Ten adres email jest powiązany z zamówieniem jako gość. Aby uzyskać pełny dostęp, zarejestruj się.",
                        "isGuestUser", true
                ));
            } else {
                // Standard "not found" response
                logger.warning("Attempt to resend verification token for non-existent user: " + email);
                return ResponseEntity.ok(Map.of("message", "Jeśli konto istnieje, nowy link aktywacyjny został wysłany"));
            }
        }

        User user = userOpt.get();

        // Check if already verified
        if (user.isVerified()) {
            logger.info("Attempted to resend verification for already verified user: " + email);
            return ResponseEntity.badRequest().body(Map.of("message", "Konto jest już zweryfikowane"));
        }

        // Send verification email
        VerificationToken token = createVerificationToken(user);
        emailService.sendVerificationEmail(user, token.getToken());

        logger.info("Verification token resent to: " + email);
        return ResponseEntity.ok(Map.of("message", "Nowy link aktywacyjny został wysłany na podany adres e-mail"));
    }
}