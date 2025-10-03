package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MapPinsRequestDto {
    private String bounds; // "sw_lat,sw_lng,ne_lat,ne_lng"
    private Integer zoom;
    private int page;
    private int perPage;
    private String city;
    private String sessionId;
    private String sortColumn = "id";
    private String sortDirection = "DESC";
}
