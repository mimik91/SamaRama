package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServicePinDto {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean verified;
    private String category;
}