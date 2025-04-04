package com.samarama.bicycle.api.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter(autoApply = true)
public class OpeningHoursConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return attribute == null ? "{}" : objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null || dbData.isEmpty() ?
                    new HashMap<>() :
                    objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}