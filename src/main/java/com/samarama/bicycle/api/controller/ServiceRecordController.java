package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.dto.ServiceRecordResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.ServiceRecord;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.service.ServiceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-records")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;
    private final BicycleRepository bicycleRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;

    public ServiceRecordController(
            ServiceRecordService serviceRecordService,
            BicycleRepository bicycleRepository,
            IncompleteBikeRepository incompleteBikeRepository
    ) {
        this.serviceRecordService = serviceRecordService;
        this.bicycleRepository = bicycleRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
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

        // Najpierw sprawdź, czy istnieje kompletny rower o podanym ID
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(bicycleId);

        if (bicycleOpt.isPresent()) {
            // Jeśli znaleziono kompletny rower, pobierz jego historię serwisową
            ResponseEntity<List<ServiceRecord>> response = serviceRecordService.getBicycleServiceRecords(bicycleId, currentUserEmail);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ServiceRecordResponseDto> dtos = response.getBody().stream()
                        .map(ServiceRecordResponseDto::fromEntity)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(dtos);
            }

            return ResponseEntity.status(response.getStatusCode()).build();
        } else {
            // Jeśli nie znaleziono kompletnego roweru, sprawdź czy istnieje niekompletny rower
            Optional<IncompleteBike> incompleteBikeOpt = incompleteBikeRepository.findById(bicycleId);

            if (incompleteBikeOpt.isPresent()) {
                // Dla niekompletnych rowerów, zwracamy pustą listę historii serwisowej
                // Można by też zwrócić komunikat, że rower musi mieć numer ramy, by mieć historię serwisową
                return ResponseEntity.ok(List.of());
            }

            // Jeśli nie znaleziono żadnego roweru, zwróć 404
            return ResponseEntity.notFound().build();
        }
    }
}