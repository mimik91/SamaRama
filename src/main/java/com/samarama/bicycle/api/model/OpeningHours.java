package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "opening_hours")
public class OpeningHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;
    private String saturday;
    private String sunday;

    @OneToOne
    @JoinColumn(name = "bike_service_id")
    private BikeService bikeService;
}