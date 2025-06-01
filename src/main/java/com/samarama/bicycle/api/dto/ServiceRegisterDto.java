package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO dla rejestracji serwisu z legacy endpoint
 */
@Data
public class ServiceRegisterDto {
    @NotBlank
    private String name;

    @NotBlank
    private String phoneNumber;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String serviceName;
}

