package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceSlotAvailabilityDto;
import com.samarama.bicycle.api.dto.ServiceSlotConfigDto;
import com.samarama.bicycle.api.model.ServiceSlotConfig;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.ServiceSlotConfigRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ServiceSlotServiceImpl implements ServiceSlotService {

    private static final Logger logger = Logger.getLogger(ServiceSlotServiceImpl.class.getName());

    private final ServiceSlotConfigRepository slotConfigRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    // Domyślne wartości konfiguracji slotów
    private static final int DEFAULT_MAX_BIKES_PER_DAY = 5;
    private static final int DEFAULT_MAX_BIKES_PER_ORDER = 3;

    @Autowired
    public ServiceSlotServiceImpl(
            ServiceSlotConfigRepository slotConfigRepository,
            ServiceOrderRepository serviceOrderRepository) {
        this.slotConfigRepository = slotConfigRepository;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    @PostConstruct
    public void init() {
        initializeDefaultSlotConfig();
    }

    @Override
    public List<ServiceSlotConfigDto> getAllSlotConfigs() {
        return slotConfigRepository.findAll().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceSlotConfigDto> getCurrentlyActiveSlotConfigs() {
        return slotConfigRepository.findCurrentlyActiveConfigs().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceSlotConfigDto> getFutureSlotConfigs() {
        return slotConfigRepository.findFutureConfigs().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> createSlotConfig(ServiceSlotConfigDto configDto) {
        // Walidacja dat
        if (configDto.startDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data początkowa jest wymagana"));
        }

        if (configDto.endDate() != null && configDto.endDate().isBefore(configDto.startDate())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data końcowa nie może być wcześniejsza niż data początkowa"));
        }

        // Walidacja liczby rowerów
        if (configDto.maxBikesPerDay() == null || configDto.maxBikesPerDay() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Maksymalna liczba rowerów dziennie musi być dodatnia"));
        }

        // Walidacja maksymalnej liczby rowerów na zamówienie
        if (configDto.maxBikesPerOrder() != null && configDto.maxBikesPerOrder() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Maksymalna liczba rowerów na zamówienie musi być dodatnia"));
        }

        if (configDto.maxBikesPerOrder() != null && configDto.maxBikesPerOrder() > configDto.maxBikesPerDay()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Maksymalna liczba rowerów na zamówienie nie może być większa niż maksymalna liczba rowerów dziennie"
            ));
        }

        // Sprawdzenie nakładających się aktywnych konfiguracji
        List<ServiceSlotConfig> overlappingConfigs = slotConfigRepository.findOverlappingConfigs(
                configDto.startDate(),
                configDto.endDate()
        );

        if (!overlappingConfigs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Istnieją już konfiguracje dla podanego zakresu dat"
            ));
        }

        // Tworzenie nowej konfiguracji
        ServiceSlotConfig newConfig = new ServiceSlotConfig();
        newConfig.setStartDate(configDto.startDate());
        newConfig.setEndDate(configDto.endDate());
        newConfig.setMaxBikesPerDay(configDto.maxBikesPerDay());
        newConfig.setMaxBikesPerOrder(configDto.maxBikesPerOrder());
        newConfig.setCreatedAt(LocalDate.now());

        ServiceSlotConfig savedConfig = slotConfigRepository.save(newConfig);
        return ResponseEntity.ok(Map.of(
                "message", "Konfiguracja slotów została utworzona pomyślnie",
                "id", savedConfig.getId()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateSlotConfig(Long id, ServiceSlotConfigDto configDto) {
        Optional<ServiceSlotConfig> configOpt = slotConfigRepository.findById(id);
        if (configOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceSlotConfig config = configOpt.get();

        // Walidacja dat
        if (configDto.startDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data początkowa jest wymagana"));
        }

        if (configDto.endDate() != null && configDto.endDate().isBefore(configDto.startDate())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data końcowa nie może być wcześniejsza niż data początkowa"));
        }

        // Walidacja liczby rowerów
        if (configDto.maxBikesPerDay() == null || configDto.maxBikesPerDay() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Maksymalna liczba rowerów dziennie musi być dodatnia"));
        }

        // Walidacja maksymalnej liczby rowerów na zamówienie
        if (configDto.maxBikesPerOrder() != null && configDto.maxBikesPerOrder() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Maksymalna liczba rowerów na zamówienie musi być dodatnia"));
        }

        if (configDto.maxBikesPerOrder() != null && configDto.maxBikesPerOrder() > configDto.maxBikesPerDay()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Maksymalna liczba rowerów na zamówienie nie może być większa niż maksymalna liczba rowerów dziennie"
            ));
        }

        // Sprawdzenie nakładających się aktywnych konfiguracji (z wyjątkiem aktualnej)
        List<ServiceSlotConfig> overlappingConfigs = slotConfigRepository.findOverlappingConfigs(
                        configDto.startDate(),
                        configDto.endDate()
                ).stream()
                .filter(c -> !c.getId().equals(id)) // Wykluczenie aktualnej konfiguracji
                .collect(Collectors.toList());

        if (!overlappingConfigs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Istnieją już inne konfiguracje dla podanego zakresu dat"
            ));
        }

        // Aktualizacja konfiguracji
        config.setStartDate(configDto.startDate());
        config.setEndDate(configDto.endDate());
        config.setMaxBikesPerDay(configDto.maxBikesPerDay());
        config.setMaxBikesPerOrder(configDto.maxBikesPerOrder());

        slotConfigRepository.save(config);
        return ResponseEntity.ok(Map.of("message", "Konfiguracja slotów została zaktualizowana pomyślnie"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteSlotConfig(Long id) {
        if (!slotConfigRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        slotConfigRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Konfiguracja slotów została usunięta pomyślnie"));
    }

    @Override
    public ServiceSlotAvailabilityDto getSlotAvailability(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        int maxBikesPerDay = getMaxBikesPerDay(date);
        int maxBikesPerOrder = getMaxBikesPerOrder(date);
        int bookedBikes = serviceOrderRepository.countBikesScheduledForDate(date);

        return ServiceSlotAvailabilityDto.of(date, maxBikesPerDay, bookedBikes, maxBikesPerOrder);
    }

    @Override
    public List<ServiceSlotAvailabilityDto> getSlotAvailability(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        if (endDate == null) {
            endDate = startDate.plusMonths(1);
        }

        // Pobieramy informacje o zarezerwowanych rowerach w danym zakresie dat
        List<Object[]> bookings = serviceOrderRepository.countBikesScheduledForDateRange(startDate, endDate);
        Map<LocalDate, Integer> bookingsMap = new HashMap<>();

        for (Object[] row : bookings) {
            LocalDate date = (LocalDate) row[0];
            Number count = (Number) row[1];
            bookingsMap.put(date, count.intValue());
        }

        // Przygotowanie wyników
        List<ServiceSlotAvailabilityDto> results = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate; // Dla lambda

            int maxBikesPerDay = getMaxBikesPerDay(date);
            int maxBikesPerOrder = getMaxBikesPerOrder(date);
            int bookedBikes = bookingsMap.getOrDefault(date, 0);

            results.add(ServiceSlotAvailabilityDto.of(date, maxBikesPerDay, bookedBikes, maxBikesPerOrder));

            currentDate = currentDate.plusDays(1);
        }

        return results;
    }

    @Override
    public List<ServiceSlotAvailabilityDto> getNextDaysAvailability(LocalDate startDate, int days) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        LocalDate endDate = startDate.plusDays(days - 1);
        return getSlotAvailability(startDate, endDate);
    }

    @Override
    public boolean areSlotsAvailable(LocalDate date, int bikesCount) {
        if (date == null) {
            return false;
        }

        int maxBikesPerDay = getMaxBikesPerDay(date);
        int bookedBikes = serviceOrderRepository.countBikesScheduledForDate(date);

        return (bookedBikes + bikesCount) <= maxBikesPerDay;
    }

    @Override
    public boolean isWithinMaxBikesPerOrder(LocalDate date, int bikesCount) {
        if (date == null) {
            return false;
        }

        int maxBikesPerOrder = getMaxBikesPerOrder(date);
        return bikesCount <= maxBikesPerOrder;
    }

    @Override
    public int getMaxBikesPerOrder(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        Optional<ServiceSlotConfig> configOpt = slotConfigRepository.findConfigForDate(date);
        if (configOpt.isPresent()) {
            ServiceSlotConfig config = configOpt.get();
            return config.getEffectiveMaxBikesPerOrder();
        }

        return DEFAULT_MAX_BIKES_PER_ORDER;
    }

    @Override
    public int getMaxBikesPerDay(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        Optional<ServiceSlotConfig> configOpt = slotConfigRepository.findConfigForDate(date);
        if (configOpt.isPresent()) {
            ServiceSlotConfig config = configOpt.get();
            return config.getMaxBikesPerDay();
        }

        return DEFAULT_MAX_BIKES_PER_DAY;
    }

    @Override
    @Transactional
    public void initializeDefaultSlotConfig() {
        // Sprawdź, czy istnieje już jakakolwiek konfiguracja
        if (slotConfigRepository.count() > 0) {
            logger.info("Konfiguracje slotów już istnieją, pomijanie inicjalizacji domyślnej konfiguracji");
            return;
        }

        logger.info("Inicjalizacja domyślnej konfiguracji slotów");

        LocalDate now = LocalDate.now();

        // Tworzenie domyślnej konfiguracji
        ServiceSlotConfig defaultConfig = new ServiceSlotConfig();
        defaultConfig.setStartDate(now);
        defaultConfig.setEndDate(null); // Bezterminowo
        defaultConfig.setMaxBikesPerDay(DEFAULT_MAX_BIKES_PER_DAY);
        defaultConfig.setMaxBikesPerOrder(DEFAULT_MAX_BIKES_PER_ORDER);
        defaultConfig.setCreatedAt(now);

        slotConfigRepository.save(defaultConfig);

        logger.info("Domyślna konfiguracja slotów została zainicjalizowana");
    }
}