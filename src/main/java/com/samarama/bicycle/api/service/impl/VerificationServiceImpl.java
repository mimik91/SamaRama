package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.model.VerificationToken;
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
import java.util.logging.Logger;

@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger logger = Logger.getLogger(VerificationServiceImpl.class.getName());

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.verification.token.expiration:86400000}")
    private long tokenExpirationMs;

    public VerificationServiceImpl(
            VerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public VerificationToken createVerificationToken(User user) {
        // Najpierw sprawdź, czy istnieje już token dla tego użytkownika
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);

        VerificationToken verificationToken;
        if (existingTokenOpt.isPresent()) {
            // Jeśli token już istnieje, zaktualizuj go
            verificationToken = existingTokenOpt.get();
            verificationToken.setToken(java.util.UUID.randomUUID().toString());
            verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(tokenExpirationMs / 60000));
            verificationToken.setUsed(false);
        } else {
            // Jeśli nie, utwórz nowy token
            verificationToken = new VerificationToken(user, (int) (tokenExpirationMs / 60000));
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

        if (verificationToken.isUsed()) {
            logger.warning("Token already used: " + token);
            return ResponseEntity.badRequest().body(Map.of("message", "Token został już wykorzystany"));
        }

        if (verificationToken.isExpired()) {
            logger.warning("Token expired: " + token);
            return ResponseEntity.badRequest().body(Map.of("message", "Token weryfikacyjny wygasł"));
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
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Dla bezpieczeństwa nie ujawniamy, że użytkownik nie istnieje
            logger.warning("Attempt to resend verification token for non-existent user: " + email);
            return ResponseEntity.ok(Map.of("message", "Jeśli konto istnieje, nowy link aktywacyjny został wysłany"));
        }

        User user = userOpt.get();

        if (user.isVerified()) {
            logger.info("Attempted to resend verification for already verified user: " + email);
            return ResponseEntity.badRequest().body(Map.of("message", "Konto jest już zweryfikowane"));
        }

        // Tworzenie nowego tokenu i wysyłanie maila
        VerificationToken token = createVerificationToken(user);
        emailService.sendVerificationEmail(user, token.getToken());

        logger.info("Verification token resent to: " + email);
        return ResponseEntity.ok(Map.of("message", "Nowy link aktywacyjny został wysłany na podany adres e-mail"));
    }
}