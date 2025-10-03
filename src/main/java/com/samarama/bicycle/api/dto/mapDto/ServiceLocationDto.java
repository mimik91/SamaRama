package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceLocationDto {
    private Long id;  // zmień String na Long
    private String name;
    private Double latitude;   // dodaj
    private Double longitude;  // dodaj
    private String address;    // dodaj jako String
    private String phoneNumber; // dodaj (z address.telephone)
    private String email;
    private Boolean verified;
    private String category;
    private String region;
}