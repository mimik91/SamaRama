package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnhancedBikeServicePinDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String phoneNumber;
    private String email;
    private String region;
    private boolean verified;
    private boolean active;
    private Double transportCost;
    private boolean transportAvailable;
    private String category;
    private int servicesCount;
    private Double rating;
}