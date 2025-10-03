package com.samarama.bicycle.api.dto.mapDto;

public record CitySuggestionDto(
        String name,
        String displayName,
        Double latitude,
        Double longitude,
        String type
) {}