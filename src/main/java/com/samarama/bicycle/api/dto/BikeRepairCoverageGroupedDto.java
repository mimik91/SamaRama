package com.samarama.bicycle.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeRepairCoverageGroupedDto {

    private List<CategoryWithCoverages> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryWithCoverages {
        private Long id;
        private String name;
        private Integer displayOrder;
        private List<BikeRepairCoverageDto> coverages;
    }
}