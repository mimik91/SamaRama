package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

public interface BikeServiceService {
    /**
     * Get a bike service by its ID
     * @param id the bike service ID
     * @return the bike service or empty if not found
     */
    Optional<BikeService> getBikeServiceById(Long id);

    /**
     * Update a bike service's information
     * @param id the bike service ID
     * @param bikeServiceDto the new bike service data
     * @return response with the result of the operation
     */
    ResponseEntity<?> updateBikeService(Long id, BikeServiceDto bikeServiceDto);

    /**
     * Update a bike service's opening hours
     * @param id the bike service ID
     * @param openingHoursData the new opening hours data
     * @return response with the result of the operation
     */
    ResponseEntity<?> updateOpeningHours(Long id, Map<String, String> openingHoursData);

    /**
     * Get a bike service's opening hours
     * @param id the bike service ID
     * @return response with the opening hours data
     */
    ResponseEntity<?> getOpeningHours(Long id);
}