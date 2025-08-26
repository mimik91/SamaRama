package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceSlotAvailabilityDto;
import com.samarama.bicycle.api.dto.ServiceSlotConfigDto;
import com.samarama.bicycle.api.model.ServiceSlotConfig;
import com.samarama.bicycle.api.repository.ServiceSlotConfigRepository;
import com.samarama.bicycle.api.repository.TransportOrderRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ServiceSlotServiceImpl implements ServiceSlotService {

    private static final Logger logger = Logger.getLogger(ServiceSlotServiceImpl.class.getName());

    private final ServiceSlotConfigRepository configRepository;
    private final TransportOrderRepository transportOrderRepository;

    // Default configuration values
    private static final int DEFAULT_MAX_BIKES_PER_DAY = 5;
    private static final int DEFAULT_MAX_BIKES_PER_ORDER = 3;

    public ServiceSlotServiceImpl(
            ServiceSlotConfigRepository configRepository,
            TransportOrderRepository transportOrderRepository) {
        this.configRepository = configRepository;
        this.transportOrderRepository = transportOrderRepository;
    }

    @PostConstruct
    public void initializeDefaultConfig() {
        initializeDefaultSlotConfig();
    }

    // === CONFIGURATION MANAGEMENT ===

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSlotConfigDto> getAllSlotConfigs() {
        return configRepository.findAll().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .sorted(Comparator.comparing(ServiceSlotConfigDto::startDate))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSlotConfigDto> getCurrentlyActiveSlotConfigs() {
        return configRepository.findCurrentlyActiveConfigs().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSlotConfigDto> getFutureSlotConfigs() {
        return configRepository.findFutureConfigs().stream()
                .map(ServiceSlotConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> createSlotConfig(ServiceSlotConfigDto configDto) {
        try {
            // Validation
            validateSlotConfig(configDto);

            // Check for overlapping configurations
            List<ServiceSlotConfig> overlapping = configRepository.findOverlappingConfigsWithEndDate(
                    configDto.startDate(), configDto.endDate());

            if (!overlapping.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Konfiguracja nakłada się z istniejącymi konfiguracjami",
                        "overlappingConfigs", overlapping.stream()
                                .map(ServiceSlotConfigDto::fromEntity)
                                .collect(Collectors.toList())
                ));
            }

            // Create new configuration
            ServiceSlotConfig config = new ServiceSlotConfig();
            config.setStartDate(configDto.startDate());
            config.setEndDate(configDto.endDate());
            config.setMaxBikesPerDay(configDto.maxBikesPerDay());
            config.setMaxBikesPerOrder(configDto.maxBikesPerOrder());
            config.setCreatedAt(LocalDate.now());

            ServiceSlotConfig savedConfig = configRepository.save(config);

            logger.info("Created new slot configuration: " + savedConfig.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Konfiguracja slotów została utworzona pomyślnie",
                    "config", ServiceSlotConfigDto.fromEntity(savedConfig)
            ));

        } catch (Exception e) {
            logger.severe("Error creating slot configuration: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateSlotConfig(Long id, ServiceSlotConfigDto configDto) {
        try {
            Optional<ServiceSlotConfig> configOpt = configRepository.findById(id);
            if (configOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ServiceSlotConfig config = configOpt.get();

            // Validation
            validateSlotConfig(configDto);

            // Check for overlapping configurations (excluding current one) using safer method
            List<ServiceSlotConfig> overlapping = findOverlappingConfigsSafely(
                    configDto.startDate(), configDto.endDate(), id);

            if (!overlapping.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Konfiguracja nakłada się z istniejącymi konfiguracjami"
                ));
            }

            // Update configuration
            config.setStartDate(configDto.startDate());
            config.setEndDate(configDto.endDate());
            config.setMaxBikesPerDay(configDto.maxBikesPerDay());
            config.setMaxBikesPerOrder(configDto.maxBikesPerOrder());

            ServiceSlotConfig savedConfig = configRepository.save(config);

            logger.info("Updated slot configuration: " + id);

            return ResponseEntity.ok(Map.of(
                    "message", "Konfiguracja slotów została zaktualizowana",
                    "config", ServiceSlotConfigDto.fromEntity(savedConfig)
            ));

        } catch (Exception e) {
            logger.severe("Error updating slot configuration " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private List<ServiceSlotConfig> findOverlappingConfigsSafely(
            LocalDate startDate, LocalDate endDate, Long excludeConfigId) {

        List<ServiceSlotConfig> overlapping;

        if (endDate != null) {
            // Użyj standardowej metody gdy endDate nie jest null
            overlapping = configRepository.findOverlappingConfigsWithEndDate(startDate, endDate);
        } else {
            // Gdy endDate jest null (konfiguracja bezterminowa), sprawdź wszystkie konflikty
            overlapping = configRepository.findOverlappingConfigsWithoutEndDate(startDate);
        }

        // Wyklucz aktualną konfigurację jeśli podano ID
        if (excludeConfigId != null) {
            overlapping = overlapping.stream()
                    .filter(config -> !config.getId().equals(excludeConfigId))
                    .collect(Collectors.toList());
        }

        return overlapping;
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteSlotConfig(Long id) {
        try {
            if (!configRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            configRepository.deleteById(id);
            logger.info("Deleted slot configuration: " + id);

            return ResponseEntity.ok(Map.of("message", "Konfiguracja slotów została usunięta"));

        } catch (Exception e) {
            logger.severe("Error deleting slot configuration " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // === AVAILABILITY CHECKING ===

    @Override
    @Transactional(readOnly = true)
    public ServiceSlotAvailabilityDto getSlotAvailability(LocalDate date) {
        ServiceSlotConfig config = getConfigForDate(date);
        int bookedBikes = countBookedBikesForDate(date);

        return ServiceSlotAvailabilityDto.of(
                date,
                config.getMaxBikesPerDay(),
                bookedBikes,
                config.getEffectiveMaxBikesPerOrder()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSlotAvailabilityDto> getSlotAvailability(LocalDate startDate, LocalDate endDate) {
        List<ServiceSlotAvailabilityDto> availability = new ArrayList<>();

        // Get all bookings for the date range in one query
        Map<LocalDate, Integer> bookingsMap = getBookingsForDateRange(startDate, endDate);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            ServiceSlotConfig config = getConfigForDate(currentDate);
            int bookedBikes = bookingsMap.getOrDefault(currentDate, 0);

            availability.add(ServiceSlotAvailabilityDto.of(
                    currentDate,
                    config.getMaxBikesPerDay(),
                    bookedBikes,
                    config.getEffectiveMaxBikesPerOrder()
            ));

            currentDate = currentDate.plusDays(1);
        }

        return availability;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSlotAvailabilityDto> getNextDaysAvailability(LocalDate startDate, int days) {
        LocalDate endDate = startDate.plusDays(days - 1);
        return getSlotAvailability(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areSlotsAvailable(LocalDate date, int bikesCount) {
        ServiceSlotConfig config = getConfigForDate(date);
        int bookedBikes = countBookedBikesForDate(date);
        int availableBikes = config.getMaxBikesPerDay() - bookedBikes;

        return availableBikes >= bikesCount;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinMaxBikesPerOrder(LocalDate date, int bikesCount) {
        ServiceSlotConfig config = getConfigForDate(date);
        return bikesCount <= config.getEffectiveMaxBikesPerOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxBikesPerOrder(LocalDate date) {
        ServiceSlotConfig config = getConfigForDate(date);
        return config.getEffectiveMaxBikesPerOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxBikesPerDay(LocalDate date) {
        ServiceSlotConfig config = getConfigForDate(date);
        return config.getMaxBikesPerDay();
    }

    // === INITIALIZATION ===

    @Override
    @Transactional
    public void initializeDefaultSlotConfig() {
        // Check if any configuration exists
        if (configRepository.count() == 0) {
            ServiceSlotConfig defaultConfig = new ServiceSlotConfig();
            defaultConfig.setStartDate(LocalDate.now());
            defaultConfig.setEndDate(null); // No end date - valid indefinitely
            defaultConfig.setMaxBikesPerDay(DEFAULT_MAX_BIKES_PER_DAY);
            defaultConfig.setMaxBikesPerOrder(DEFAULT_MAX_BIKES_PER_ORDER);
            defaultConfig.setCreatedAt(LocalDate.now());

            configRepository.save(defaultConfig);
            logger.info("Initialized default slot configuration");
        }
    }

    @Override
    public int countOrderOnDate(LocalDate date) {
        int ans = transportOrderRepository.countByPickupDate(date);
        return ans;
    }

    // === PRIVATE HELPER METHODS ===

    /**
     * Gets the active configuration for a specific date
     */
    private ServiceSlotConfig getConfigForDate(LocalDate date) {
        return configRepository.findConfigForDate(date)
                .orElseGet(() -> {
                    // If no configuration found, return default values
                    ServiceSlotConfig defaultConfig = new ServiceSlotConfig();
                    defaultConfig.setMaxBikesPerDay(DEFAULT_MAX_BIKES_PER_DAY);
                    defaultConfig.setMaxBikesPerOrder(DEFAULT_MAX_BIKES_PER_ORDER);
                    return defaultConfig;
                });
    }

    /**
     * Counts booked bikes for a specific date
     * Uses TransportOrderRepository which includes both transport and service orders
     */
    private int countBookedBikesForDate(LocalDate date) {
        return transportOrderRepository.countBikesScheduledForDate(date);
    }

    /**
     * Gets bookings for a date range as a map
     */
    private Map<LocalDate, Integer> getBookingsForDateRange(LocalDate startDate, LocalDate endDate) {
        // Use the existing method from TransportOrderRepository
        List<Object[]> bookings = transportOrderRepository.countOrdersForDateRange(startDate, endDate);

        Map<LocalDate, Integer> bookingsMap = new HashMap<>();
        for (Object[] booking : bookings) {
            LocalDate date = (LocalDate) booking[0];
            Long count = (Long) booking[1];
            bookingsMap.put(date, count.intValue());
        }

        return bookingsMap;
    }

    /**
     * Validates slot configuration data
     */
    private void validateSlotConfig(ServiceSlotConfigDto configDto) {
        if (configDto.startDate() == null) {
            throw new IllegalArgumentException("Data początkowa jest wymagana");
        }

        if (configDto.maxBikesPerDay() == null || configDto.maxBikesPerDay() <= 0) {
            throw new IllegalArgumentException("Maksymalna liczba rowerów na dzień musi być większa od 0");
        }

        if (configDto.maxBikesPerOrder() != null && configDto.maxBikesPerOrder() <= 0) {
            throw new IllegalArgumentException("Maksymalna liczba rowerów na zamówienie musi być większa od 0");
        }

        if (configDto.maxBikesPerOrder() != null &&
                configDto.maxBikesPerOrder() > configDto.maxBikesPerDay()) {
            throw new IllegalArgumentException("Maksymalna liczba rowerów na zamówienie nie może być większa niż na dzień");
        }

        if (configDto.endDate() != null && configDto.endDate().isBefore(configDto.startDate())) {
            throw new IllegalArgumentException("Data końcowa nie może być wcześniejsza niż data początkowa");
        }

        // Business rule: start date cannot be in the past (except for current configs)
        if (configDto.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data początkowa nie może być w przeszłości");
        }
    }

    /**
     * Checks if a date range overlaps with existing configurations
     */
    private boolean hasOverlappingConfigs(LocalDate startDate, LocalDate endDate, Long excludeConfigId) {
        List<ServiceSlotConfig> overlapping = configRepository.findOverlappingConfigsWithEndDate(startDate, endDate);

        if (excludeConfigId != null) {
            overlapping = overlapping.stream()
                    .filter(config -> !config.getId().equals(excludeConfigId))
                    .collect(Collectors.toList());
        }

        return !overlapping.isEmpty();
    }

    /**
     * Gets statistics about slot utilization
     */
    public Map<String, Object> getSlotUtilizationStats(LocalDate startDate, LocalDate endDate) {
        List<ServiceSlotAvailabilityDto> availability = getSlotAvailability(startDate, endDate);

        int totalDays = availability.size();
        int fullyBookedDays = (int) availability.stream()
                .filter(a -> !a.isAvailable())
                .count();

        double averageUtilization = availability.stream()
                .mapToDouble(a -> a.maxBikesPerDay() > 0 ?
                        (double) a.bookedBikes() / a.maxBikesPerDay() * 100 : 0)
                .average()
                .orElse(0.0);

        return Map.of(
                "totalDays", totalDays,
                "fullyBookedDays", fullyBookedDays,
                "averageUtilization", String.format("%.1f%%", averageUtilization),
                "availableDays", totalDays - fullyBookedDays
        );
    }
}