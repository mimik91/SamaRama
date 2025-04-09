package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bicycle_photos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BicyclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id")
    @JsonIgnore
    @ToString.Exclude
    private IncompleteBike bike;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo_data")
    @ToString.Exclude
    private byte[] photoData;

    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public void setPhotoFromFile(byte[] data, String contentType, Long fileSize) {
        this.photoData = data;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = LocalDateTime.now();
    }
}