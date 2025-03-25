package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.ServiceRecord;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.ServiceRecordRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-records")
public class ServiceRecordController {
    private final ServiceRecordRepository serviceRecordRepository;
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;
    private final BikeServiceRepository bikeServiceRepository;

    public ServiceRecordController(ServiceRecordRepository serviceRecordRepository,
                                   BicycleRepository bicycleRepository,
                                   UserRepository userRepository, BikeServiceRepository bikeServiceRepository) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @GetMapping("/bicycle/{bicycleId}")
    public ResponseEntity<List<ServiceRecord>> getBicycleServiceRecords(@PathVariable Long bicycleId) {
        Bicycle bicycle = bicycleRepository.findById(bicycleId)
                .orElseThrow(() -> new RuntimeException("Bicycle not found"));

        // Check if user is owner or a serviceman
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.UserRole.SERVICEMAN &&
                (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId()))) {
            return ResponseEntity.status(403).build();
        }

        List<ServiceRecord> records = serviceRecordRepository.findByBicycle(bicycle);
        return ResponseEntity.ok(records);
    }

    @PostMapping
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> addServiceRecord(@Valid @RequestBody ServiceRecordDto serviceRecordDto) {
        // Validate service date is not more than a month in the past
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        if (serviceRecordDto.serviceDate().isBefore(oneMonthAgo)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Service date cannot be more than a month ago"));
        }

        Bicycle bicycle = bicycleRepository.findById(serviceRecordDto.bicycleId())
                .orElseThrow(() -> new RuntimeException("Bicycle not found"));

        String email = getCurrentUserEmail();
        BikeService service = serviceRecordDto.service();

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setBicycle(bicycle);
        serviceRecord.setName(serviceRecordDto.name());
        serviceRecord.setDescription(serviceRecordDto.description());
        serviceRecord.setServiceDate(serviceRecordDto.serviceDate());
        serviceRecord.setPrice(serviceRecordDto.price());
        serviceRecord.setService(serviceRecordDto.service());

        serviceRecordRepository.save(serviceRecord);
        return ResponseEntity.ok(Map.of("message", "Service record added successfully"));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
