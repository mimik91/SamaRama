package com.samarama.bicycle.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bike-services")
public class BikeServiceController {
    private final BikeServiceRepository bikeServiceRepository;
    private final ObjectMapper objectMapper;

    public BikeServiceController(BikeServiceRepository bikeServiceRepository, ObjectMapper objectMapper) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<BikeService>> getAllBikeServices() {
        List<BikeService> services = bikeServiceRepository.findAll();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BikeService> getBikeServiceById(@PathVariable Long id) {
        Optional<BikeService> service = bikeServiceRepository.findById(id);
        return service.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> createBikeService(@Valid @RequestBody BikeServiceDto bikeServiceDto) {
        try {
            BikeService bikeService = new BikeService();
            bikeService.setName(bikeServiceDto.name());
            bikeService.setAddress(bikeServiceDto.address());
            bikeService.setPostalCode(bikeServiceDto.postalCode());
            bikeService.setCity(bikeServiceDto.city());
            bikeService.setPhoneNumber(bikeServiceDto.phoneNumber());
            bikeService.setEmail(bikeServiceDto.email());
            bikeService.setDescription(bikeServiceDto.description());

            // Convert opening hours map to JSON string
            String openingHoursJson = objectMapper.writeValueAsString(bikeServiceDto.openingHours());
            bikeService.setOpeningHours(openingHoursJson);

            bikeServiceRepository.save(bikeService);
            return ResponseEntity.ok(Map.of("message", "Bike service created successfully"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid opening hours format"));
        }
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity updateBikeService(@PathVariable Long id, @Valid @RequestBody BikeServiceDto bikeServiceDto) {
        Optional<BikeService> existingService = bikeServiceRepository.findById(id);

        if (existingService.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            BikeService bikeService = existingService.get();
            bikeService.setName(bikeServiceDto.name());
            bikeService.setAddress(bikeServiceDto.address());
            bikeService.setPostalCode(bikeServiceDto.postalCode());
            bikeService.setCity(bikeServiceDto.city());
            bikeService.setPhoneNumber(bikeServiceDto.phoneNumber());
            bikeService.setEmail(bikeServiceDto.email());
            bikeService.setDescription(bikeServiceDto.description());

            // Convert opening hours map to JSON string
            String openingHoursJson = objectMapper.writeValueAsString(bikeServiceDto.openingHours());
            bikeService.setOpeningHours(openingHoursJson);

            bikeServiceRepository.save(bikeService);
            return ResponseEntity.ok(Map.of("message", "Bike service updated successfully"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid opening hours format"));
        }
    }
}