package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.PasswordResetDto;
import com.samarama.bicycle.api.dto.PasswordResetRequestDto;
import org.springframework.http.ResponseEntity;

public interface PasswordResetService {
    /**
     * Inicjuje proces resetowania hasła
     * @param requestDto zawiera email użytkownika
     * @return odpowiedź HTTP z informacją o wyniku operacji
     */
    ResponseEntity<?> requestPasswordReset(PasswordResetRequestDto requestDto);

    /**
     * Resetuje hasło użytkownika na podstawie tokenu
     * @param resetDto zawiera token i nowe hasło
     * @return odpowiedź HTTP z informacją o wyniku operacji
     */
    ResponseEntity<?> resetPassword(PasswordResetDto resetDto);
}