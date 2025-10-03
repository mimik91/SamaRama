package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MapPinsResponseDto {
    private List<ServicePinDto> data;
    private int total;
    private BoundsDto bounds;
    private String cache;
}