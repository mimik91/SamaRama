package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Reprezentuje konfigurację liczby dostępnych slotów serwisowych na określony okres
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_slot_configs")
public class ServiceSlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Data, od której obowiązuje konfiguracja
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Data, do której obowiązuje konfiguracja (null oznacza bezterminowo)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Maksymalna liczba rowerów, które warsztat może obsłużyć danego dnia
     */
    @NotNull
    @Positive
    @Column(name = "max_bikes_per_day", nullable = false)
    private Integer maxBikesPerDay;

    /**
     * Maksymalna liczba rowerów, które można zamówić w jednym zamówieniu
     * (jeśli null, to domyślnie równa maxBikesPerDay)
     */
    @Positive
    @Column(name = "max_bikes_per_order")
    private Integer maxBikesPerOrder;

    @Column(name = "created_at")
    private LocalDate createdAt = LocalDate.now();

    /**
     * Sprawdza czy konfiguracja jest aktywna dla podanej daty
     * @param date data do sprawdzenia
     * @return true jeśli konfiguracja jest aktywna dla podanej daty
     */
    public boolean isActiveForDate(LocalDate date) {
        return !date.isBefore(startDate) &&
                (endDate == null || !date.isAfter(endDate));
    }

    /**
     * Zwraca efektywną wartość maxBikesPerOrder
     * @return wartość maxBikesPerOrder lub maxBikesPerDay jeśli maxBikesPerOrder jest null
     */
    public int getEffectiveMaxBikesPerOrder() {
        return maxBikesPerOrder != null ? maxBikesPerOrder : maxBikesPerDay;
    }
}