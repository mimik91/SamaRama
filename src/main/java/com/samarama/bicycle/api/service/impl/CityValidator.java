package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.service.BicycleEnumerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for city values in service orders.
 * Ensures that only cities from the CITY enumeration are accepted.
 */
@Component
public class CityValidator {

    private final BicycleEnumerationService enumerationService;

    @Autowired
    public CityValidator(BicycleEnumerationService enumerationService) {
        this.enumerationService = enumerationService;
    }

    /**
     * Validates if the given city is in the CITY enumeration
     * @param city City name to validate
     * @return true if city is valid, false otherwise
     */
    public boolean isValidCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return false;
        }

        List<String> validCities = enumerationService.getEnumerationValues("CITY");
        return validCities.contains(city);
    }

    /**
     * Extracts city from the pickup address string
     * Expected format: "Street, City"
     * @param pickupAddress Full pickup address
     * @return City name or null if not found
     */
    public String extractCityFromAddress(String pickupAddress) {
        if (pickupAddress == null || pickupAddress.trim().isEmpty()) {
            return null;
        }

        // Split by comma and get the last part which should be the city
        String[] parts = pickupAddress.split(",");
        if (parts.length < 2) {
            return null;
        }

        return parts[parts.length - 1].trim();
    }
}