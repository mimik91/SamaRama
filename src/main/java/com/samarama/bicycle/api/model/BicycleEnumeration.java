package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bicycle_enumerations")
public class BicycleEnumeration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // Nazwa kolumny w tabeli
    private Long id;

    @Column(name = "type", unique = true, nullable = false) // Dodana nazwa kolumny
    private String type; // np. "BRAND", "BIKE_TYPE", "FRAME_MATERIAL"

    @ElementCollection
    @CollectionTable(
            name = "bicycle_enumeration_values",
            joinColumns = @JoinColumn(name = "enumeration_id")
    )
    @Column(name = "value") // Nazwa kolumny dla wartości w kolekcji
    private List<String> values = new ArrayList<>();

    // Konstruktor pomocniczy
    public BicycleEnumeration(String type, List<String> values) {
        this.type = type;
        this.values = values;
    }
}