package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.OpeningHours;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.OpeningHoursRepository;
import com.samarama.bicycle.api.service.BikeServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class BikeServiceServiceImpl implements BikeServiceService {
    private static final Logger logger = Logger.getLogger(BikeServiceServiceImpl.class.getName());

    private final BikeServiceRepository bikeServiceRepository;
    private final OpeningHoursRepository openingHoursRepository;

    public BikeServiceServiceImpl(BikeServiceRepository bikeServiceRepository,
                                  OpeningHoursRepository openingHoursRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.openingHoursRepository = openingHoursRepository;
    }

    @Override
    public Optional<BikeService> getBikeServiceById(Long id) {
        logger.info("Fetching bike service with ID: " + id);
        Optional<BikeService> service = bikeServiceRepository.findById(id);

        if (service.isPresent()) {
            logger.info("Found bike service: " + service.get().getName());
        } else {
            logger.warning("Bike service with ID " + id + " not found");
        }

        return service;
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateBikeService(Long id, BikeServiceDto bikeServiceDto) {
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

    @Override
    @Transactional
    public ResponseEntity<?> updateOpeningHours(Long id, Map<String, String> openingHoursData) {
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

    @Override
    public ResponseEntity<?> getOpeningHours(Long id) {
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

        Map<String, String> result = new HashMap<>();
        result.put("monday", openingHours.getMonday() != null ? openingHours.getMonday() : "");
        result.put("tuesday", openingHours.getTuesday() != null ? openingHours.getTuesday() : "");
        result.put("wednesday", openingHours.getWednesday() != null ? openingHours.getWednesday() : "");
        result.put("thursday", openingHours.getThursday() != null ? openingHours.getThursday() : "");
        result.put("friday", openingHours.getFriday() != null ? openingHours.getFriday() : "");
        result.put("saturday", openingHours.getSaturday() != null ? openingHours.getSaturday() : "");
        result.put("sunday", openingHours.getSunday() != null ? openingHours.getSunday() : "");

        return ResponseEntity.ok(result);
    }
}