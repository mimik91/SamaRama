package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientInfoDto {
    private String userAgent;
    private String referer;
    private String ipAddress;
    private String screenResolution;
}
