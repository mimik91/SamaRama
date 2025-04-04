package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.OpeningHours;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.OpeningHoursRepository;
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
    private final OpeningHoursRepository openingHoursRepository;

    public BikeServiceController(BikeServiceRepository bikeServiceRepository,
                                 OpeningHoursRepository openingHoursRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.openingHoursRepository = openingHoursRepository;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> updateBikeService(@PathVariable Long id, @Valid @RequestBody BikeServiceDto bikeServiceDto) {
        Optional<BikeService> existingService = bikeServiceRepository.findById(id);

        if (existingService.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            BikeService bikeService = existingService.get();
            bikeService.setName(bikeServiceDto.name());
            bikeService.setStreet(bikeServiceDto.street());
            bikeService.setBuilding(bikeServiceDto.building());
            bikeService.setFlat(bikeServiceDto.flat());
            bikeService.setPostalCode(bikeServiceDto.postalCode());
            bikeService.setCity(bikeServiceDto.city());
            bikeService.setPhoneNumber(bikeServiceDto.phoneNumber());
            bikeService.setBusinessPhone(bikeServiceDto.businessPhone());
            bikeService.setEmail(bikeServiceDto.email());
            bikeService.setDescription(bikeServiceDto.description());
            bikeService.setLatitude(bikeServiceDto.latitude());
            bikeService.setLongitude(bikeServiceDto.longitude());

            bikeServiceRepository.save(bikeService);
            return ResponseEntity.ok(Map.of("message", "Bike service updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error updating bike service: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/opening-hours")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> updateOpeningHours(@PathVariable Long id, @RequestBody Map<String, String> openingHoursData) {
        Optional<BikeService> bikeServiceOpt = bikeServiceRepository.findById(id);
        if (bikeServiceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            BikeService bikeService = bikeServiceOpt.get();
            OpeningHours openingHours = bikeService.getOpeningHours();

            if (openingHours == null) {
                openingHours = new OpeningHours();
                openingHours.setBikeService(bikeService);
            }

            // Update opening hours for each day
            openingHours.setMonday(openingHoursData.get("monday"));
            openingHours.setTuesday(openingHoursData.get("tuesday"));
            openingHours.setWednesday(openingHoursData.get("wednesday"));
            openingHours.setThursday(openingHoursData.get("thursday"));
            openingHours.setFriday(openingHoursData.get("friday"));
            openingHours.setSaturday(openingHoursData.get("saturday"));
            openingHours.setSunday(openingHoursData.get("sunday"));

            openingHoursRepository.save(openingHours);
            return ResponseEntity.ok(Map.of("message", "Opening hours updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error updating opening hours: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/opening-hours")
    public ResponseEntity<?> getOpeningHours(@PathVariable Long id) {
        Optional<BikeService> bikeServiceOpt = bikeServiceRepository.findById(id);
        if (bikeServiceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BikeService bikeService = bikeServiceOpt.get();
        OpeningHours openingHours = bikeService.getOpeningHours();

        if (openingHours == null) {
            return ResponseEntity.ok(Map.of(
                    "monday", "",
                    "tuesday", "",
                    "wednesday", "",
                    "thursday", "",
                    "friday", "",
                    "saturday", "",
                    "sunday", ""
            ));
        }

        Map<String, String> result = Map.of(
                "monday", openingHours.getMonday() != null ? openingHours.getMonday() : "",
                "tuesday", openingHours.getTuesday() != null ? openingHours.getTuesday() : "",
                "wednesday", openingHours.getWednesday() != null ? openingHours.getWednesday() : "",
                "thursday", openingHours.getThursday() != null ? openingHours.getThursday() : "",
                "friday", openingHours.getFriday() != null ? openingHours.getFriday() : "",
                "saturday", openingHours.getSaturday() != null ? openingHours.getSaturday() : "",
                "sunday", openingHours.getSunday() != null ? openingHours.getSunday() : ""
        );

        return ResponseEntity.ok(result);
    }
}