package com.samarama.bicycle.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BikeRepairCoverageMapDto {

    @JsonProperty("coveragesByCategory")
    private Map<BikeRepairCoverageCategoryDto, List<BikeRepairCoverageDto>> coveragesByCategory;
}