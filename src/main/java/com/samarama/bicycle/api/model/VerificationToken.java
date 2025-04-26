package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime expiryDate;

    private boolean used;

    // Konstruktor tworzący token z domyślną datą wygaśnięcia (24h)
    public VerificationToken(User user) {
        this.token = UUID.randomUUID().toString();
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.used = false;
    }

    // Konstruktor tworzący token z określoną datą wygaśnięcia (w minutach)
    public VerificationToken(User user, int expirationTimeInMinutes) {
        this.token = UUID.randomUUID().toString();
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(expirationTimeInMinutes);
        this.used = false;
    }

    // Metoda sprawdzająca, czy token wygasł
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}