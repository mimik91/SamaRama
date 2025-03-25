package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank String email,
        @NotBlank String password
) {
}
