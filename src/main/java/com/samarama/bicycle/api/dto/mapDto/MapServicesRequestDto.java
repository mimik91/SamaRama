package com.samarama.bicycle.api.dto.mapDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MapServicesRequestDto {
    private String type = "event";

    @JsonProperty("payload")
    private PayloadDto payload;

    private String bounds;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer perPage = 25;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayloadDto {
        private String website;
        private String screen;
        private String hostname;
        private String language;
        private String referrer;
        private String title;
        private String url;
    }
}