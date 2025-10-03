package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MapSearchRequestDto {
    private String query;
    private String bounds;
    private int page;
    private int perPage;
    private boolean verifiedOnly;
    private String sessionId;
    private String city;
}
