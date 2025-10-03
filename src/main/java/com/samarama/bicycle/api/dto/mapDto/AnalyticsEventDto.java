package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsEventDto {
    private String eventType;
    private String serviceId;
    private String sessionId;
    private Long timestamp;
    private Map<String, Object> properties;
}
