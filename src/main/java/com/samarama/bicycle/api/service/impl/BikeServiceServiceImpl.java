package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.BikeServiceService;
import com.samarama.bicycle.api.service.helper.csvReader.BikeServiceCsvImporter;
import com.samarama.bicycle.api.service.helper.BikeServiceValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BikeServiceServiceImpl implements BikeServiceService {

    private static final Logger logger = Logger.getLogger(BikeServiceServiceImpl.class.getName());


    private final BikeServiceValidator bikeServiceValidator;
    private final BikeServiceRegisteredRepository bikeServiceRegisteredRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final BikeServiceCsvImporter csvImporter;
    private final BikeServiceValidator validator;

    @Autowired
    public BikeServiceServiceImpl( BikeServiceValidator bikeServiceValidator, BikeServiceRegisteredRepository bikeServiceRegisteredRepository, ServiceUserRepository serviceUserRepository, BikeServiceRepository bikeServiceRepository,
                                  BikeServiceCsvImporter csvImporter,
                                  BikeServiceValidator validator) {
        this.bikeServiceValidator = bikeServiceValidator;
        this.bikeServiceRegisteredRepository = bikeServiceRegisteredRepository;
        this.serviceUserRepository = serviceUserRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.csvImporter = csvImporter;
        this.validator = validator;
    }

    @PersistenceContext
    private EntityManager em;

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
            em.flush();
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

    // === NOWE IMPLEMENTACJE METOD ===

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyBikeServiceRegisteredDetails(String userEmail) {
        try {
            logger.info("Pobieranie szczegółów zarejestrowanego serwisu dla użytkownika: " + userEmail);

            // Znajdź użytkownika serwisu
            Optional<ServiceUser> serviceUserOpt = serviceUserRepository.findByEmail(userEmail);
            if (serviceUserOpt.isEmpty()) {
                logger.warning("Nie znaleziono użytkownika serwisu: " + userEmail);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nie znaleziono użytkownika serwisu"));
            }

            ServiceUser serviceUser = serviceUserOpt.get();

            // Sprawdź czy użytkownik ma przypisany serwis
            if (serviceUser.getBikeServiceId() == null) {
                logger.info("Użytkownik " + userEmail + " nie ma przypisanego serwisu");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Użytkownik nie ma przypisanego serwisu"));
            }

            // Pobierz szczegóły serwisu
            Optional<BikeServiceRegistered> serviceOpt = bikeServiceRegisteredRepository
                    .findById(serviceUser.getBikeServiceId());
            if (serviceOpt.isEmpty()) {
                logger.warning("Nie znaleziono serwisu o ID: " + serviceUser.getBikeServiceId() +
                        " dla użytkownika: " + userEmail);
                return ResponseEntity.notFound().build();
            }

            BikeServiceRegisteredDto serviceDto = BikeServiceRegisteredDto.fromEntity(serviceOpt.get());
            logger.info("Pomyślnie pobrano dane serwisu dla użytkownika: " + userEmail);
            return ResponseEntity.ok(serviceDto);

        } catch (Exception e) {
            logger.severe("Błąd podczas pobierania danych serwisu dla użytkownika " + userEmail + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas pobierania danych serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> registerMyBikeService(BikeServiceRegisteredDto bikeServiceRegisteredDto) {
        try {

            // Walidacja danych
            ResponseEntity<?> validationError = bikeServiceValidator.validateForCreation(bikeServiceRegisteredDto);
            if (validationError != null) {
                logger.info("Błąd walidacji podczas rejestracji serwisu");
                return validationError;
            }

            // Utworzenie nowego zarejestrowanego serwisu
            BikeServiceRegistered bikeServiceRegistered = bikeServiceRegisteredDto.toEntity();
            Optional<BikeService> bikeServiceOpt = bikeServiceRepository.findServiceByEmailIgnoreCase(bikeServiceRegisteredDto.email());
            if(bikeServiceOpt.isEmpty()){
                bikeServiceOpt = bikeServiceRepository.findServiceByNameIgnoreCase(bikeServiceRegisteredDto.name());
            }
            if(bikeServiceOpt.isPresent()){
                bikeServiceRegistered.setLatitude(bikeServiceOpt.get().getLatitude());
                bikeServiceRegistered.setLongitude(bikeServiceRegistered.getLongitude());
                deleteBikeService(bikeServiceOpt.get().getId());
            }
            BikeServiceRegistered savedService = bikeServiceRegisteredRepository.save(bikeServiceRegistered);



            logger.info("Pomyślnie utworzono serwis: " + savedService.getName() +
                    " (ID: " + savedService.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis rowerowy został zarejestrowany pomyślnie",
                    "id", savedService.getId(),
                    "service", BikeServiceRegisteredDto.fromEntity(savedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas rejestracji serwisu");
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas rejestracji serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateMyBikeServiceRegistered(BikeServiceRegisteredDto bikeServiceRegisteredDto, String userEmail) {
        try {
            logger.info("Rozpoczęcie aktualizacji serwisu dla użytkownika: " + userEmail);

            // Znajdź użytkownika serwisu
            Optional<ServiceUser> serviceUserOpt = serviceUserRepository.findByEmail(userEmail);
            if (serviceUserOpt.isEmpty()) {
                logger.warning("Nie znaleziono użytkownika serwisu: " + userEmail);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nie znaleziono użytkownika serwisu"));
            }

            ServiceUser serviceUser = serviceUserOpt.get();

            // Sprawdź czy użytkownik ma przypisany serwis
            if (serviceUser.getBikeServiceId() == null) {
                logger.warning("Użytkownik " + userEmail + " nie ma przypisanego serwisu do aktualizacji");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Użytkownik nie ma przypisanego serwisu do aktualizacji"));
            }

            // Znajdź istniejący serwis
            Optional<BikeServiceRegistered> existingServiceOpt = bikeServiceRegisteredRepository
                    .findById(serviceUser.getBikeServiceId());
            if (existingServiceOpt.isEmpty()) {
                logger.warning("Nie znaleziono serwisu o ID: " + serviceUser.getBikeServiceId() +
                        " dla użytkownika: " + userEmail);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nie znaleziono serwisu do aktualizacji"));
            }

            BikeServiceRegistered existingService = existingServiceOpt.get();

            // Walidacja danych
            ResponseEntity<?> validationError = bikeServiceValidator.validateForUpdate(bikeServiceRegisteredDto, existingService);
            if (validationError != null) {
                logger.info("Błąd walidacji podczas aktualizacji serwisu dla użytkownika: " + userEmail);
                return validationError;
            }

            // Aktualizacja danych serwisu
            bikeServiceRegisteredDto.updateEntity(existingService);
            BikeServiceRegistered updatedService = bikeServiceRegisteredRepository.save(existingService);

            logger.info("Pomyślnie zaktualizowano serwis: " + updatedService.getName() +
                    " (ID: " + updatedService.getId() + ") dla użytkownika: " + userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Dane serwisu zostały zaktualizowane pomyślnie",
                    "service", BikeServiceRegisteredDto.fromEntity(updatedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas aktualizacji serwisu dla użytkownika " + userEmail + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas aktualizacji serwisu: " + e.getMessage()));
        }
    }

    @Override
    public boolean isSuffixTaken(String suffix) {
        return bikeServiceRegisteredRepository.existsBySuffixIgnoreCase(suffix);
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