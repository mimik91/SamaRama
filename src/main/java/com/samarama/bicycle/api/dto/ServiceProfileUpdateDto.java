package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceProfileUpdateDto(
        @NotBlank String name,
        String street,
        String building,
        String flat,
        String postalCode,
        String city,
        @Size(min = 9, max = 9) String phoneNumber,
        String businessPhone,
        String description
) {
}