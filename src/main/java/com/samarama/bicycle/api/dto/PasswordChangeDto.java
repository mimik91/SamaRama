package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeDto(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6) String newPassword
) {
}