package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoundsDto {
    private PointDto sw; // southwest corner
    private PointDto ne; // northeast corner
    private PointDto center;
    private int zoom;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PointDto {
        private double latitude;
        private double longitude;
    }
}