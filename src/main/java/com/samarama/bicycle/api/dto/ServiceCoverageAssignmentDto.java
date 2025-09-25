package com.samarama.bicycle.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCoverageAssignmentDto {

    @JsonProperty("existingCoverageIds")
    private List<Long> existingCoverageIds;

    @JsonProperty("newCategories")
    private List<NewCategoryDto> newCategories;

    @JsonProperty("customCoverages")
    private List<CustomCoverageDto> customCoverages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewCategoryDto {
        @JsonProperty("name")
        private String name;

        @JsonProperty("displayOrder") // opcjonalne
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomCoverageDto {
        @JsonProperty("categoryName") // zmiana z categoryId na categoryName
        private String categoryName;

        @JsonProperty("coverageName")
        private String coverageName;
    }
}