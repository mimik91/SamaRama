package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.model.ServiceRecord;
import com.samarama.bicycle.api.service.ServiceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-records")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping("/bicycle/{bicycleId}")
    public ResponseEntity<List<ServiceRecord>> getBicycleServiceRecords(@PathVariable Long bicycleId) {
        return serviceRecordService.getBicycleServiceRecords(bicycleId);
    }

    @PostMapping
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> addServiceRecord(@Valid @RequestBody ServiceRecordDto serviceRecordDto) {
        String email = getCurrentUserEmail();
        return serviceRecordService.addServiceRecord(serviceRecordDto, email);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}