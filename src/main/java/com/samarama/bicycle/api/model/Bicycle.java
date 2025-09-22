package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "bicycles")
@PrimaryKeyJoinColumn(name = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bicycle extends IncompleteBike {

    @Column(name = "frame_number", unique = true)
    private String frameNumber;

    public Bicycle(Long id, String brand, String model, String type, String frameMaterial,
                   LocalDate productionDate, IndividualUser owner, String frameNumber) {
        super(id, brand, model, type, frameMaterial, null, owner, LocalDateTime.now(), productionDate);
        this.frameNumber = frameNumber;
    }
}