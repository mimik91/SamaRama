package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incomplete_bikes")
@Inheritance(strategy = InheritanceType.JOINED)
@DynamicInsert
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IncompleteBike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String brand;

    private String model;

    private String type;

    private String frameMaterial;

    @OneToOne(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private BicyclePhoto photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    @ToString.Exclude
    private IncompleteUser owner;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDate productionDate;

    public boolean hasPhoto() {
        return photo != null && photo.getPhotoData() != null && photo.getPhotoData().length > 0;
    }
}