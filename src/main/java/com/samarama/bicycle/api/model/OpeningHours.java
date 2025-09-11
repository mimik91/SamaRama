package com.samarama.bicycle.api.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(name = "opening_hours")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpeningHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Użycie @OneToMany z @MapKeyEnumerated do mapowania klucza mapy (DayOfWeek)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "opening_hours_id")
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "day_of_week")
    private Map<DayOfWeek, OpeningInterval> intervals = new EnumMap<>(DayOfWeek.class);

    // Wygodne metody do dodawania i usuwania
    public void addInterval(DayOfWeek day, OpeningInterval interval) {
        if (interval != null) {
            this.intervals.put(day, interval);
        }
    }

    public void removeInterval(DayOfWeek day) {
        this.intervals.remove(day);
    }
}