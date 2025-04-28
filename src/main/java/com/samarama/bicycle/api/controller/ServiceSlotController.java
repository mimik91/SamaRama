package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceSlotAvailabilityDto;
import com.samarama.bicycle.api.dto.ServiceSlotConfigDto;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ServiceSlotService slotService;
    private final ServiceOrderRepository serviceOrderRepository;

    @Autowired
    public ServiceSlotController(ServiceSlotService slotService, ServiceOrderRepository serviceOrderRepository) {
        this.slotService = slotService;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    /**
     * Pobiera dostępność slotów dla określonej daty
     */
    @GetMapping("/availability")
    public ResponseEntity<ServiceSlotAvailabilityDto> getSlotAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ServiceSlotAvailabilityDto availability = slotService.getSlotAvailability(date != null ? date : LocalDate.now());
        return ResponseEntity.ok(availability);
    }

    /**
     * Pobiera dostępność slotów dla zakresu dat
     */
    @GetMapping("/availability/range")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getSlotAvailabilityRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ServiceSlotAvailabilityDto> availabilityList = slotService.getSlotAvailability(
                startDate != null ? startDate : LocalDate.now(),
                endDate != null ? endDate : LocalDate.now().plusMonths(1)
        );

        return ResponseEntity.ok(availabilityList);
    }

    /**
     * Pobiera dostępność slotów na kolejne X dni
     */
    @GetMapping("/availability/next-days")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getNextDaysAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "30") int days) {

        List<ServiceSlotAvailabilityDto> availabilityList = slotService.getNextDaysAvailability(
                startDate != null ? startDate : LocalDate.now(),
                days
        );

        return ResponseEntity.ok(availabilityList);
    }

    /**
     * Sprawdza, czy dla danej daty i liczby rowerów są dostępne sloty
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int bikesCount) {

        boolean available = slotService.areSlotsAvailable(date, bikesCount);
        boolean withinLimit = slotService.isWithinMaxBikesPerOrder(date, bikesCount);

        if (!withinLimit) {
            int maxBikesPerOrder = slotService.getMaxBikesPerOrder(date);
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "reason", "MAX_BIKES_PER_ORDER_EXCEEDED",
                    "message", "Przekroczono maksymalną liczbę rowerów na zamówienie (" + maxBikesPerOrder + ")",
                    "maxBikesPerOrder", maxBikesPerOrder
            ));
        }

        if (!available) {
            int maxBikesPerDay = slotService.getMaxBikesPerDay(date);
            int availableBikes = maxBikesPerDay - serviceOrderRepository.countBikesScheduledForDate(date);

            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "reason", "NO_AVAILABLE_SLOTS",
                    "message", "Brak wystarczającej liczby dostępnych slotów na wybrany dzień",
                    "availableBikes", Math.max(0, availableBikes),
                    "maxBikesPerDay", maxBikesPerDay
            ));
        }

        return ResponseEntity.ok(Map.of("available", true));
    }

    // Endpointy dla administratora do zarządzania konfiguracjami slotów

    /**
     * Pobiera wszystkie konfiguracje slotów
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceSlotConfigDto>> getAllSlotConfigs() {
        List<ServiceSlotConfigDto> configs = slotService.getAllSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Pobiera aktywne konfiguracje slotów
     */
    @GetMapping("/config/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceSlotConfigDto>> getActiveSlotConfigs() {
        List<ServiceSlotConfigDto> configs = slotService.getCurrentlyActiveSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Pobiera przyszłe konfiguracje slotów
     */
    @GetMapping("/config/future")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceSlotConfigDto>> getFutureSlotConfigs() {
        List<ServiceSlotConfigDto> configs = slotService.getFutureSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Tworzy nową konfigurację slotów
     */
    @PostMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> createSlotConfig(@Valid @RequestBody ServiceSlotConfigDto configDto) {
        return slotService.createSlotConfig(configDto);
    }

    /**
     * Aktualizuje istniejącą konfigurację slotów
     */
    @PutMapping("/config/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateSlotConfig(
            @PathVariable Long id,
            @Valid @RequestBody ServiceSlotConfigDto configDto) {

        return slotService.updateSlotConfig(id, configDto);
    }

    /**
     * Usuwa konfigurację slotów
     */
    @DeleteMapping("/config/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteSlotConfig(@PathVariable Long id) {
        return slotService.deleteSlotConfig(id);
    }
}