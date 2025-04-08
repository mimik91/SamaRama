package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bicycles")
@DynamicInsert
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bicycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String frameNumber;

    @NotBlank
    private String brand;

    private String model;

    private String type;

    private String frameMaterial;

    @OneToOne(mappedBy = "bicycle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BicyclePhoto photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDate productionDate;

    @OneToMany(mappedBy = "bicycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ServiceRecord> serviceRecords = new HashSet<>();

    public boolean hasPhoto() {
        return photo != null;
    }
}