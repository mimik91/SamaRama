package com.samarama.bicycle.api.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "opening_intervals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpeningInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacja Many-to-One do OpeningHours
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_hours_id", nullable = false)
    private OpeningHours openingHours;

    @Column(name = "open_time", nullable = false)
    private String openTime;

    @Column(name = "close_time", nullable = false)
    private String closeTime;
}