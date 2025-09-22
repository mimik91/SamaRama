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
    @Column(name = "id") // Nazwa kolumny
    private Long id;

    @NotBlank
    @Column(name = "brand") // Nazwa kolumny
    private String brand;

    @Column(name = "model") // Nazwa kolumny
    private String model;

    @Column(name = "type") // Nazwa kolumny
    private String type;

    @Column(name = "frame_material") // Nazwa kolumny
    private String frameMaterial;

    @OneToOne(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private BicyclePhoto photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id") // Nazwa kolumny
    @JsonIgnore
    @ToString.Exclude
    private IndividualUser owner;

    @Column(name = "created_at") // Nazwa kolumny
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "production_date") // Nazwa kolumny
    private LocalDate productionDate;

    public boolean hasPhoto() {
        return photo != null && photo.getPhotoData() != null && photo.getPhotoData().length > 0;
    }
}