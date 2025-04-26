package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.model.User;

public interface EmailService {
    /**
     * Wysyła mail z linkiem aktywacyjnym
     * @param user użytkownik do którego wysyłamy mail
     * @param token token weryfikacyjny
     */
    void sendVerificationEmail(User user, String token);

    /**
     * Wysyła mail potwierdzający aktywację konta
     * @param user użytkownik którego konto zostało aktywowane
     */
    void sendAccountActivatedEmail(User user);
}