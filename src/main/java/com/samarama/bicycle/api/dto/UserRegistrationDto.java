package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationDto(@NotBlank @Size(min = 2, max = 50) @Email String email, @NotBlank @Size(min = 2, max = 50)String firstName, @NotBlank @Size(min = 2, max = 50) String lastName, @Size(min = 9, max = 9) String phoneNumber, @NotBlank @Size(min = 5, max = 25) String password) {
}
