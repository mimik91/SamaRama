package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceUserDto(
        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Email musi mieć prawidłowy format")
        String email,

        @NotBlank(message = "Hasło jest wymagane")
        @Size(min = 6, max = 120, message = "Hasło musi mieć między 6 a 120 znaków")
        String password,

        @NotNull(message = "ID serwisu rowerowego jest wymagane")
        Long bikeServiceId
){}