package com.samarama.bicycle.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BikeRepairCoverageDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    @NotBlank(message = "Coverage name cannot be blank")
    @Size(max = 255, message = "Coverage name cannot exceed 255 characters")
    private String name;

    @JsonProperty("categoryId")
    @NotNull(message = "Category ID is required")
    private Long categoryId;


}