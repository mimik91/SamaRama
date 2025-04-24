package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServicePackageDto;
import com.samarama.bicycle.api.model.ServicePackage;
import com.samarama.bicycle.api.service.ServicePackageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-packages")
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    @Autowired
    public ServicePackageController(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    /**
     * Pobierz wszystkie pakiety serwisowe (publiczny endpoint)
     */
    @GetMapping
    public ResponseEntity<List<ServicePackageDto>> getAllServicePackages() {
        List<ServicePackageDto> packages = servicePackageService.getAllServicePackages().stream()
                .map(ServicePackageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    /**
     * Pobierz tylko aktywne pakiety serwisowe (publiczny endpoint)
     */
    @GetMapping("/active")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ServicePackageDto>> getActiveServicePackages() {
        List<ServicePackageDto> packages = servicePackageService.getActiveServicePackages().stream()
                .map(ServicePackageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    /**
     * Pobierz pakiet serwisowy po identyfikatorze
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageDto> getServicePackageById(@PathVariable Long id) {
        Optional<ServicePackage> packageOpt = servicePackageService.getServicePackageById(id);
        return packageOpt
                .map(p -> ResponseEntity.ok(ServicePackageDto.fromEntity(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pobierz pakiet serwisowy po kodzie
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ServicePackageDto> getServicePackageByCode(@PathVariable String code) {
        Optional<ServicePackage> packageOpt = servicePackageService.getServicePackageByCode(code);
        return packageOpt
                .map(p -> ResponseEntity.ok(ServicePackageDto.fromEntity(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Utwórz nowy pakiet serwisowy (tylko dla administratorów i moderatorów)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> createServicePackage(@Valid @RequestBody ServicePackageDto servicePackageDto) {
        return servicePackageService.createServicePackage(servicePackageDto);
    }

    /**
     * Zaktualizuj istniejący pakiet serwisowy (tylko dla administratorów i moderatorów)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateServicePackage(
            @PathVariable Long id,
            @Valid @RequestBody ServicePackageDto servicePackageDto) {
        return servicePackageService.updateServicePackage(id, servicePackageDto);
    }

    /**
     * Usuń pakiet serwisowy (tylko dla administratorów)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteServicePackage(@PathVariable Long id) {
        return servicePackageService.deleteServicePackage(id);
    }

    /**
     * Aktywuj lub dezaktywuj pakiet serwisowy (tylko dla administratorów i moderatorów)
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> toggleServicePackageActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Wymagane pole 'active'"));
        }
        return servicePackageService.toggleServicePackageActive(id, active);
    }

    /**
     * Zainicjuj domyślne pakiety serwisowe (tylko dla administratorów)
     */
    @PostMapping("/initialize-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initializeDefaultServicePackages() {
        servicePackageService.initializeDefaultServicePackages();
        return ResponseEntity.ok(Map.of("message", "Domyślne pakiety serwisowe zostały zainicjalizowane"));
    }
}