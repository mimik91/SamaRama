package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.service.BikeServiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bike-services")
public class BikeServiceController {
    private static final Logger logger = Logger.getLogger(BikeServiceController.class.getName());

    private final BikeServiceService bikeServiceService;

    public BikeServiceController(BikeServiceService bikeServiceService) {
        this.bikeServiceService = bikeServiceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BikeService> getBikeServiceById(@PathVariable Long id) {
        Optional<BikeService> service = bikeServiceService.getBikeServiceById(id);
        return service.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> updateBikeService(@PathVariable Long id, @Valid @RequestBody BikeServiceDto bikeServiceDto) {
        return bikeServiceService.updateBikeService(id, bikeServiceDto);
    }

    @PutMapping("/{id}/opening-hours")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> updateOpeningHours(@PathVariable Long id, @RequestBody Map<String, String> openingHoursData) {
        return bikeServiceService.updateOpeningHours(id, openingHoursData);
    }

    @GetMapping("/{id}/opening-hours")
    public ResponseEntity<?> getOpeningHours(@PathVariable Long id) {
        return bikeServiceService.getOpeningHours(id);
    }
}