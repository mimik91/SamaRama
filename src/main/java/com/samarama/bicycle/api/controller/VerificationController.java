package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    @Autowired
    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Endpoint do weryfikacji konta na podstawie tokenu
     * @param token token weryfikacyjny
     * @return odpowiedź z wynikiem weryfikacji
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam String token) {
        return verificationService.verifyAccount(token);
    }

    /**
     * Endpoint do ponownego wysłania maila weryfikacyjnego
     * @param email adres email użytkownika
     * @return odpowiedź z wynikiem operacji
     */
    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationToken(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email jest wymagany"));
        }
        return verificationService.resendVerificationToken(email);
    }
}