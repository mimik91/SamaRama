package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BikeServiceDto(
        @NotBlank String name,
        String street,
        String building,
        String flat,
        String postalCode,
        String city,
        String phoneNumber,
        String businessPhone,
        @Email String email,
        Double latitude,
        Double longitude,
        String description,
        @NotBlank @Size(min = 5) String password
) {
}