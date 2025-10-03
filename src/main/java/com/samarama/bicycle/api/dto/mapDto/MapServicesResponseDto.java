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
public class MapServicesResponseDto {
    private List<ServiceLocationDto> data;
    private int total;
    private int totalPages;
    private String sortColumn;
    private String sortDirection;
    private int page;
    private int previous;
    private int next;
    private int perPage;
    private BoundsDto bounds;
    private String cache;
}