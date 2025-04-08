package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.dto.ServiceRecordResponseDto;
import com.samarama.bicycle.api.model.ServiceRecord;
import com.samarama.bicycle.api.service.ServiceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-records")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
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

    @GetMapping("/bicycle/{bicycleId}")
    public ResponseEntity<List<ServiceRecordResponseDto>> getBicycleServiceRecords(@PathVariable Long bicycleId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        ResponseEntity<List<ServiceRecord>> response = serviceRecordService.getBicycleServiceRecords(bicycleId, currentUserEmail);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<ServiceRecordResponseDto> dtos = response.getBody().stream()
                    .map(ServiceRecordResponseDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }

        return ResponseEntity.status(response.getStatusCode()).build();
    }
}