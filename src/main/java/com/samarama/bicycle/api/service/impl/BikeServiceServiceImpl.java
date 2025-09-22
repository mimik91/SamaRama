package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServicePinDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.BikeServiceService;
import com.samarama.bicycle.api.service.helper.csvReader.BikeServiceCsvImporter;
import com.samarama.bicycle.api.service.helper.BikeServiceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class BikeServiceServiceImpl implements BikeServiceService {

    private static final Logger logger = Logger.getLogger(BikeServiceServiceImpl.class.getName());

    private final BikeServiceRepository bikeServiceRepository;
    private final BikeServiceCsvImporter csvImporter;
    private final BikeServiceValidator validator;

    @Autowired
    public BikeServiceServiceImpl(BikeServiceRepository bikeServiceRepository,
                                  BikeServiceCsvImporter csvImporter,
                                  BikeServiceValidator validator) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.csvImporter = csvImporter;
        this.validator = validator;
    }

    // === PUBLICZNE METODY ===

    @Override
    @Transactional(readOnly = true)
    public List<BikeServicePinDto> getAllBikeServicePins() {
        logger.info("Pobieranie pinów wszystkich serwisów rowerowych");

        return bikeServiceRepository.findServicesWithCoordinates().stream()
                .map(BikeServicePinDto::fromEntity)
                .filter(BikeServicePinDto::hasValidCoordinates)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BikeServiceDto> getBikeServiceDetails(Long id) {
        logger.info("Pobieranie szczegółów serwisu rowerowego o ID: " + id);

        return bikeServiceRepository.findById(id)
                .map(BikeServiceDto::fromEntity);
    }

    // === METODY ADMINISTRACYJNE ===

    @Override
    @Transactional(readOnly = true)
    public List<BikeServiceDto> getAllBikeServicesForAdmin() {
        logger.info("Pobieranie wszystkich serwisów rowerowych dla administratora");

        return bikeServiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BikeServiceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> createBikeService(BikeServiceDto bikeServiceDto) {
        try {
            // Walidacja danych
            ResponseEntity<?> validationError = validator.validateForCreation(bikeServiceDto);
            if (validationError != null) {
                return validationError;
            }

            // Utworzenie nowego serwisu
            BikeService bikeService = bikeServiceDto.toEntity();
            BikeService savedService = bikeServiceRepository.save(bikeService);

            logger.info("Utworzono nowy serwis rowerowy: " + savedService.getName() + " (ID: " + savedService.getId() + ")");

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis rowerowy został utworzony pomyślnie",
                    "id", savedService.getId(),
                    "service", BikeServiceDto.fromEntity(savedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas tworzenia serwisu rowerowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas tworzenia serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateBikeService(Long id, BikeServiceDto bikeServiceDto) {
        try {
            Optional<BikeService> existingServiceOpt = bikeServiceRepository.findById(id);
            if (existingServiceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BikeService existingService = existingServiceOpt.get();

            // Walidacja danych
            ResponseEntity<?> validationError = validator.validateForUpdate(bikeServiceDto, existingService);
            if (validationError != null) {
                return validationError;
            }

            // Aktualizacja danych serwisu
            updateServiceFields(existingService, bikeServiceDto);
            BikeService updatedService = bikeServiceRepository.save(existingService);

            logger.info("Zaktualizowano serwis rowerowy: " + updatedService.getName() + " (ID: " + id + ")");

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis rowerowy został zaktualizowany pomyślnie",
                    "service", BikeServiceDto.fromEntity(updatedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas aktualizacji serwisu rowerowego o ID " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas aktualizacji serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteBikeService(Long id) {
        try {
            if (!bikeServiceRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            bikeServiceRepository.deleteById(id);
            logger.info("Usunięto serwis rowerowy o ID: " + id);

            return ResponseEntity.ok(Map.of("message", "Serwis rowerowy został usunięty pomyślnie"));

        } catch (Exception e) {
            logger.severe("Błąd podczas usuwania serwisu rowerowego o ID " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas usuwania serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> importBikeServicesFromCsv(MultipartFile file, String adminEmail) {
        return csvImporter.importBikeServicesFromCsv(file, adminEmail);
    }

    @Override
    public void updateTransportPrices(String previous, String newPrice) {
        List<BikeService> bikeServices = bikeServiceRepository.findAllByTransportCost(new BigDecimal(previous));
        BigDecimal newTransportCost = new BigDecimal(newPrice);
        bikeServices.forEach(service -> service.setTransportCost(newTransportCost));
    }

    // === METODY POMOCNICZE ===

    /**
     * Aktualizuje pola obiektu BikeService na podstawie DTO
     */
    private void updateServiceFields(BikeService existingService, BikeServiceDto bikeServiceDto) {
        existingService.setName(bikeServiceDto.name());
        existingService.setEmail(bikeServiceDto.email());
        existingService.setStreet(bikeServiceDto.street());
        existingService.setBuilding(bikeServiceDto.building());
        existingService.setFlat(bikeServiceDto.flat());
        existingService.setPostalCode(bikeServiceDto.postalCode());
        existingService.setCity(bikeServiceDto.city());
        existingService.setLatitude(bikeServiceDto.latitude());
        existingService.setLongitude(bikeServiceDto.longitude());
        existingService.setPhoneNumber(bikeServiceDto.phoneNumber());
        existingService.setTransportCost(bikeServiceDto.transportCost());
        existingService.setTransportAvailable(bikeServiceDto.transportAvailable());
    }
}