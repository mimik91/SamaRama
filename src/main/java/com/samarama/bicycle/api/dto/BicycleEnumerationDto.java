package com.samarama.bicycle.api.dto;

import java.util.List;

public record BicycleEnumerationDto(String type, List<String> values) {
}