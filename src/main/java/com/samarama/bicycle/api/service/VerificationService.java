package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.model.VerificationToken;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface VerificationService {
    /**
     * Tworzy nowy token weryfikacyjny dla użytkownika
     * @param user użytkownik, dla którego tworzymy token
     * @return utworzony token weryfikacyjny
     */
    VerificationToken createVerificationToken(User user);

    /**
     * Znajduje token weryfikacyjny po wartości tokenu
     * @param token wartość tokenu
     * @return znaleziony token lub pusty optional
     */
    Optional<VerificationToken> getVerificationToken(String token);

    /**
     * Weryfikuje konto użytkownika
     * @param token wartość tokenu do weryfikacji
     * @return odpowiedź HTTP z wynikiem weryfikacji
     */
    ResponseEntity<?> verifyAccount(String token);

    /**
     * Ponownie wysyła email z tokenem weryfikacyjnym
     * @param email adres email użytkownika
     * @return odpowiedź HTTP z wynikiem operacji
     */
    ResponseEntity<?> resendVerificationToken(String email);
}