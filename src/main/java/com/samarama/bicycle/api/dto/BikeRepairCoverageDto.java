
package com.samarama.bicycle.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeRepairCoverageDto {
    private Long id;
    private String name;
    private Long categoryId; // przydatne w innych kontekstach
}