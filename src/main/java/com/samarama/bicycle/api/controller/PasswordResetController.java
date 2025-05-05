package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.PasswordResetDto;
import com.samarama.bicycle.api.dto.PasswordResetRequestDto;
import com.samarama.bicycle.api.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Endpoint do żądania resetowania hasła
     */
    @PostMapping("/reset-request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto requestDto) {
        return passwordResetService.requestPasswordReset(requestDto);
    }

    /**
     * Endpoint do resetowania hasła
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetDto resetDto) {
        return passwordResetService.resetPassword(resetDto);
    }
}