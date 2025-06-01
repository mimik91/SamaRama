package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceSlotAvailabilityDto;
import com.samarama.bicycle.api.dto.ServiceSlotConfigDto;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-slots")
public class ServiceSlotController {

    private final ServiceSlotService serviceSlotService;
    private final ServiceOrderRepository serviceOrderRepository;

    public ServiceSlotController(ServiceSlotService serviceSlotService,
                                 ServiceOrderRepository serviceOrderRepository) {
        this.serviceSlotService = serviceSlotService;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    /**
     * Pobierz wszystkie konfiguracje slotów (admin)
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceSlotConfigDto>> getAllSlotConfigs() {
        List<ServiceSlotConfigDto> configs = serviceSlotService.getAllSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Pobierz aktywne konfiguracje slotów (admin)
     */
    @GetMapping("/config/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceSlotConfigDto>> getActiveSlotConfigs() {
        List<ServiceSlotConfigDto> configs = serviceSlotService.getCurrentlyActiveSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Utwórz nową konfigurację slotów (admin)
     */
    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSlotConfig(@Valid @RequestBody ServiceSlotConfigDto configDto) {
        return serviceSlotService.createSlotConfig(configDto);
    }

    /**
     * Zaktualizuj konfigurację slotów (admin)
     */
    @PutMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSlotConfig(
            @PathVariable Long id,
            @Valid @RequestBody ServiceSlotConfigDto configDto) {
        return serviceSlotService.updateSlotConfig(id, configDto);
    }

    /**
     * Usuń konfigurację slotów (admin)
     */
    @DeleteMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSlotConfig(@PathVariable Long id) {
        return serviceSlotService.deleteSlotConfig(id);
    }

    /**
     * Pobierz dostępność slotów na dzień (publiczny)
     */
    @GetMapping("/availability/{date}")
    public ResponseEntity<ServiceSlotAvailabilityDto> getSlotAvailability(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ServiceSlotAvailabilityDto availability = serviceSlotService.getSlotAvailability(date);
        return ResponseEntity.ok(availability);
    }

    /**
     * Pobierz dostępność slotów na zakres dat (publiczny)
     */
    @GetMapping("/availability")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getSlotAvailabilityRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ServiceSlotAvailabilityDto> availability = serviceSlotService.getSlotAvailability(startDate, endDate);
        return ResponseEntity.ok(availability);
    }

    /**
     * Pobierz dostępność slotów na następne N dni (publiczny)
     */
    @GetMapping("/availability/next-days")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getNextDaysAvailability(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now();
        List<ServiceSlotAvailabilityDto> availability = serviceSlotService.getNextDaysAvailability(start, days);
        return ResponseEntity.ok(availability);
    }

    /**
     * Sprawdź dostępność slotów dla konkretnej liczby rowerów (publiczny)
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int bikesCount) {

        boolean available = serviceSlotService.areSlotsAvailable(date, bikesCount);
        int maxBikesPerDay = serviceSlotService.getMaxBikesPerDay(date);
        int maxBikesPerOrder = serviceSlotService.getMaxBikesPerOrder(date);
        int availableBikes = maxBikesPerDay - serviceOrderRepository.countByPickupDate(date); // Używamy metody z ServiceOrderRepository

        return ResponseEntity.ok(Map.of(
                "date", date,
                "available", available,
                "bikesCount", bikesCount,
                "maxBikesPerDay", maxBikesPerDay,
                "maxBikesPerOrder", maxBikesPerOrder,
                "availableBikes", Math.max(0, availableBikes),
                "message", available ?
                        "Wystarczająca liczba dostępnych slotów" :
                        "Brak wystarczającej liczby slotów"
        ));
    }

    /**
     * Pobierz statystyki wykorzystania slotów (admin)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> getSlotStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<ServiceSlotAvailabilityDto> availability = serviceSlotService.getSlotAvailability(start, end);

        // Oblicz statystyki
        int totalDays = availability.size();
        int fullyBookedDays = (int) availability.stream().filter(a -> a.availableBikes() == 0).count();
        double averageUtilization = availability.stream()
                .mapToDouble(a -> a.maxBikesPerDay() > 0 ?
                        (double) a.bookedBikes() / a.maxBikesPerDay() * 100 : 0)
                .average().orElse(0.0);

        return ResponseEntity.ok(Map.of(
                "period", Map.of("startDate", start, "endDate", end),
                "totalDays", totalDays,
                "fullyBookedDays", fullyBookedDays,
                "averageUtilization", Math.round(averageUtilization * 100.0) / 100.0,
                "utilizationPercentage", Math.round(averageUtilization) + "%",
                "availability", availability
        ));
    }

    /**
     * Zainicjuj domyślną konfigurację slotów (admin)
     */
    @PostMapping("/config/initialize-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initializeDefaultSlotConfig() {
        serviceSlotService.initializeDefaultSlotConfig();
        return ResponseEntity.ok(Map.of("message", "Domyślna konfiguracja slotów została zainicjalizowana"));
    }

    /**
     * Pobierz limity slotów dla konkretnej daty (publiczny)
     */
    @GetMapping("/limits/{date}")
    public ResponseEntity<?> getSlotLimits(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        int maxBikesPerDay = serviceSlotService.getMaxBikesPerDay(date);
        int maxBikesPerOrder = serviceSlotService.getMaxBikesPerOrder(date);
        int bookedBikes = serviceOrderRepository.countByPickupDate(date); // Używamy metody z ServiceOrderRepository
        int availableBikes = Math.max(0, maxBikesPerDay - bookedBikes);

        return ResponseEntity.ok(Map.of(
                "date", date,
                "maxBikesPerDay", maxBikesPerDay,
                "maxBikesPerOrder", maxBikesPerOrder,
                "bookedBikes", bookedBikes,
                "availableBikes", availableBikes,
                "utilizationPercentage", maxBikesPerDay > 0 ?
                        Math.round((double) bookedBikes / maxBikesPerDay * 100) : 0
        ));
    }
}