package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeRepairCoverageCategoryDto;
import com.samarama.bicycle.api.dto.BikeRepairCoverageDto;
import com.samarama.bicycle.api.dto.BikeRepairCoverageMapDto;
import com.samarama.bicycle.api.dto.ServiceCoverageAssignmentDto;
import com.samarama.bicycle.api.model.BikeRepairCoverage;
import com.samarama.bicycle.api.model.BikeRepairCoverageCategory;
import com.samarama.bicycle.api.model.BikeServiceRegistered;
import com.samarama.bicycle.api.model.ServiceUser;
import com.samarama.bicycle.api.repository.BikeRepairCoverageCategoryRepository;
import com.samarama.bicycle.api.repository.BikeRepairCoverageRepository;
import com.samarama.bicycle.api.repository.BikeServiceRegisteredRepository;
import com.samarama.bicycle.api.repository.ServiceUserRepository;
import com.samarama.bicycle.api.service.BikeRepairCoverageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class BikeRepairCoverageServiceImpl implements BikeRepairCoverageService {

    private final BikeRepairCoverageCategoryRepository bikeRepairCoverageCategoryRepository;
    private final BikeRepairCoverageRepository bikeRepairCoverageRepository;
    private final BikeServiceRegisteredRepository bikeServiceRegisteredRepository;
    private final ServiceUserRepository serviceUserRepository;

    public BikeRepairCoverageServiceImpl(BikeRepairCoverageCategoryRepository bikeRepairCoverageCategoryRepository, BikeRepairCoverageRepository bikeRepairCoverageRepository, BikeServiceRegisteredRepository bikeServiceRegisteredRepository, ServiceUserRepository serviceUserRepository) {
        this.bikeRepairCoverageCategoryRepository = bikeRepairCoverageCategoryRepository;
        this.bikeRepairCoverageRepository = bikeRepairCoverageRepository;
        this.bikeServiceRegisteredRepository = bikeServiceRegisteredRepository;
        this.serviceUserRepository = serviceUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BikeRepairCoverageMapDto getAllRepairCoverages() {
        log.info("Pobieranie wszystkich pokryć napraw pogrupowanych po kategoriach");

        try {
            List<BikeRepairCoverageCategory> categories = bikeRepairCoverageCategoryRepository.findAllByOrderByDisplayOrderAsc();
            Map<BikeRepairCoverageCategoryDto, List<BikeRepairCoverageDto>> coveragesByCategory = new LinkedHashMap<>();

            for (BikeRepairCoverageCategory category : categories) {
                BikeRepairCoverageCategoryDto categoryDto = BikeRepairCoverageCategoryDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .displayOrder(category.getDisplayOrder())
                        .build();

                List<BikeRepairCoverage> coveragesForCategory =
                        bikeRepairCoverageRepository.findByCategoryIdOrderByNameAsc(category.getId());

                List<BikeRepairCoverageDto> coverageDtos = coveragesForCategory.stream()
                        .map(coverage -> BikeRepairCoverageDto.builder()
                                .id(coverage.getId())
                                .name(coverage.getName())
                                .categoryId(coverage.getCategory().getId())
                                .build())
                        .collect(Collectors.toList());

                coveragesByCategory.put(categoryDto, coverageDtos);
            }

            log.info("Pobrano {} kategorii z pokryciami napraw", categories.size());

            return BikeRepairCoverageMapDto.builder()
                    .coveragesByCategory(coveragesByCategory)
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas pobierania wszystkich pokryć napraw", e);
            return BikeRepairCoverageMapDto.builder()
                    .coveragesByCategory(new LinkedHashMap<>())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyRepairCoverages(String userEmail) {
        log.info("Pobieranie pokryć napraw dla użytkownika: {}", userEmail);

        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.warn("Próba pobierania pokryć napraw z pustym emailem użytkownika");
            return ResponseEntity.badRequest()
                    .body("Email użytkownika jest wymagany");
        }

        try {
            Optional<ServiceUser> serviceUserOpt = serviceUserRepository.findByEmail(userEmail);
            if (serviceUserOpt.isEmpty()) {
                log.warn("Nie znaleziono użytkownika serwisu: {}", userEmail);
                return ResponseEntity.badRequest()
                        .body("Użytkownik nie jest zarejestrowany jako serwis rowerowy");
            }

            ServiceUser serviceUser = serviceUserOpt.get();
            Optional<BikeServiceRegistered> bikeServiceOpt =
                    bikeServiceRegisteredRepository.findById(serviceUser.getBikeServiceId());

            if (bikeServiceOpt.isEmpty()) {
                log.warn("Nie znaleziono zarejestrowanego serwisu dla użytkownika: {}", userEmail);
                return ResponseEntity.badRequest()
                        .body("Serwis nie jest zarejestrowany w systemie");
            }

            BikeServiceRegistered bikeService = bikeServiceOpt.get();
            Set<BikeRepairCoverage> assignedCoverages = bikeService.getBikeRepairCoverages();

            // Grupujemy po kategoriach
            Map<BikeRepairCoverageCategoryDto, List<BikeRepairCoverageDto>> coveragesByCategory =
                    assignedCoverages.stream()
                            .collect(Collectors.groupingBy(
                                    coverage -> BikeRepairCoverageCategoryDto.builder()
                                            .id(coverage.getCategory().getId())
                                            .name(coverage.getCategory().getName())
                                            .displayOrder(coverage.getCategory().getDisplayOrder())
                                            .build(),
                                    LinkedHashMap::new,
                                    Collectors.mapping(
                                            coverage -> BikeRepairCoverageDto.builder()
                                                    .id(coverage.getId())
                                                    .name(coverage.getName())
                                                    .categoryId(coverage.getCategory().getId())
                                                    .build(),
                                            Collectors.toList()
                                    )
                            ));

            BikeRepairCoverageMapDto result = BikeRepairCoverageMapDto.builder()
                    .coveragesByCategory(coveragesByCategory)
                    .build();

            log.info("Znaleziono {} przypisanych pokryć napraw dla użytkownika: {}",
                    assignedCoverages.size(), userEmail);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania pokryć napraw dla użytkownika: {}", userEmail, e);
            return ResponseEntity.internalServerError()
                    .body("Wystąpił błąd podczas pobierania pokryć napraw");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> assignMyRepairCoverages(Long bikeServiceId, ServiceCoverageAssignmentDto coverageAssignment) {
        log.info("Przypisywanie uproszczonych pokryć napraw dla serwisu ID: {}", bikeServiceId);

        try {
            BikeServiceRegistered bikeService = getBikeServiceById(bikeServiceId);
            clearAssignedBikeRepairCoverages(bikeService);

            // Krok 1: Utwórz wszystkie nowe kategorie
            Map<String, BikeRepairCoverageCategory> createdCategories = createNewCategories(
                    coverageAssignment.getNewCategories()
            );

            // Krok 2: Utwórz wszystkie niestandardowe coverage'y i zbierz ich ID
            List<Long> newCoverageIds = createCustomCoverages(
                    coverageAssignment.getCustomCoverages(),
                    createdCategories
            );

            // Krok 3: Połącz istniejące ID z nowo utworzonymi ID
            List<Long> allCoverageIds = new ArrayList<>();
            if (coverageAssignment.getExistingCoverageIds() != null) {
                allCoverageIds.addAll(coverageAssignment.getExistingCoverageIds());
            }
            allCoverageIds.addAll(newCoverageIds);

            // Krok 4: Przypisz wszystkie coverage'y do serwisu (przez ID)
            if (!allCoverageIds.isEmpty()) {
                Set<BikeRepairCoverage> coverages = loadCoveragesByIds(allCoverageIds);
                assignCoveragesToService(bikeService, coverages);
            }

            bikeServiceRegisteredRepository.save(bikeService);
            log.info("Pomyślnie przypisano {} coverage'ów do serwisu ID: {}",
                    allCoverageIds.size(), bikeServiceId);

            ResponseEntity<?> resp = createSuccessResponse(bikeService);

            return resp;

        } catch (ValidationException e) {
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Błąd podczas przypisywania uproszczonych pokryć napraw dla serwisu id: {}", bikeServiceId, e);
            return ResponseEntity.internalServerError()
                    .body("Wystąpił błąd podczas aktualizacji pokryć napraw");
        }
    }

    private Map<String, BikeRepairCoverageCategory> createNewCategories(List<ServiceCoverageAssignmentDto.NewCategoryDto> newCategories) {
        Map<String, BikeRepairCoverageCategory> createdCategories = new HashMap<>();

        if (newCategories == null || newCategories.isEmpty()) {
            return createdCategories;
        }

        for (ServiceCoverageAssignmentDto.NewCategoryDto newCategoryDto : newCategories) {
            if (newCategoryDto.getName() == null || newCategoryDto.getName().trim().isEmpty()) {
                log.warn("Pominięto kategorię bez nazwy");
                continue;
            }

            String categoryName = newCategoryDto.getName().trim();

            // Sprawdź czy już nie stworzyliśmy tej kategorii (duplikaty w żądaniu)
            if (createdCategories.containsKey(categoryName)) {
                log.debug("Kategoria '{}' już została przetworzona", categoryName);
                continue;
            }

            // Sprawdź czy kategoria już istnieje w bazie
            Optional<BikeRepairCoverageCategory> existingCategory =
                    bikeRepairCoverageCategoryRepository.findByName(categoryName);

            BikeRepairCoverageCategory category;
            if (existingCategory.isPresent()) {
                category = existingCategory.get();
                log.info("Znaleziono istniejącą kategorię: {} (ID: {})", categoryName, category.getId());
            } else {
                // Utwórz nową kategorię
                Integer maxDisplayOrder = bikeRepairCoverageCategoryRepository.findMaxDisplayOrder();
                int displayOrder = newCategoryDto.getDisplayOrder() != null ?
                        newCategoryDto.getDisplayOrder() :
                        ((maxDisplayOrder != null) ? maxDisplayOrder + 1 : 1);

                category = new BikeRepairCoverageCategory();
                category.setName(categoryName);
                category.setDisplayOrder(displayOrder);
                category.setCreatedAt(LocalDateTime.now());

                category = bikeRepairCoverageCategoryRepository.save(category);
                log.info("Utworzono nową kategorię: {} (ID: {})", categoryName, category.getId());
            }

            createdCategories.put(categoryName, category);
        }

        log.info("Przetworzono {} kategorii", createdCategories.size());
        return createdCategories;
    }


    private List<Long> createCustomCoverages(List<ServiceCoverageAssignmentDto.CustomCoverageDto> customCoverages,
                                             Map<String, BikeRepairCoverageCategory> createdCategories) {
        List<Long> newCoverageIds = new ArrayList<>();

        if (customCoverages == null || customCoverages.isEmpty()) {
            return newCoverageIds;
        }

        for (ServiceCoverageAssignmentDto.CustomCoverageDto customCoverageDto : customCoverages) {
            if (customCoverageDto.getCategoryName() == null || customCoverageDto.getCategoryName().trim().isEmpty() ||
                    customCoverageDto.getCoverageName() == null || customCoverageDto.getCoverageName().trim().isEmpty()) {
                log.warn("Pominięto nieprawidłowe niestandardowe coverage: {}", customCoverageDto);
                continue;
            }

            String categoryName = customCoverageDto.getCategoryName().trim();
            String coverageName = customCoverageDto.getCoverageName().trim();

            // Znajdź kategorię - najpierw w nowo utworzonych, potem w bazie
            BikeRepairCoverageCategory category = createdCategories.get(categoryName);
            if (category == null) {
                // Szukaj w istniejących kategoriach w bazie
                Optional<BikeRepairCoverageCategory> existingCategory =
                        bikeRepairCoverageCategoryRepository.findByName(categoryName);
                if (existingCategory.isPresent()) {
                    category = existingCategory.get();
                    log.debug("Znaleziono istniejącą kategorię dla coverage: {} -> {}",
                            coverageName, categoryName);
                } else {
                    throw new ValidationException("Nie znaleziono kategorii o nazwie: " + categoryName);
                }
            }

            // Sprawdź czy coverage już istnieje w tej kategorii
            Optional<BikeRepairCoverage> existingCoverage =
                    bikeRepairCoverageRepository.findByNameAndCategoryId(coverageName, category.getId());

            BikeRepairCoverage coverage;
            if (existingCoverage.isPresent()) {
                coverage = existingCoverage.get();
                log.debug("Znaleziono istniejące coverage: {} w kategorii: {}",
                        coverageName, categoryName);
            } else {
                // Utwórz nowe coverage
                coverage = new BikeRepairCoverage();
                coverage.setName(coverageName);
                coverage.setCategory(category);
                coverage.setCreatedAt(LocalDateTime.now());

                coverage = bikeRepairCoverageRepository.save(coverage);
                log.info("Utworzono nowe coverage: {} w kategorii: {} (ID: {})",
                        coverageName, categoryName, coverage.getId());
            }

            newCoverageIds.add(coverage.getId());
        }

        log.info("Przetworzono {} niestandardowych coverage'ów", newCoverageIds.size());
        return newCoverageIds;
    }

    private BikeServiceRegistered getBikeServiceById(Long id) {
        return bikeServiceRegisteredRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Serwis nie jest zarejestrowany w systemie"));
    }

    private void clearAssignedBikeRepairCoverages(BikeServiceRegistered bikeService) {
        bikeService.getBikeRepairCoverages().clear();
        log.debug("Wyczyszczono wszystkie istniejące pokrycia dla serwisu: {}", bikeService.getName());
    }

    private Set<BikeRepairCoverage> loadCoveragesByIds(List<Long> coverageIds) {
        if (coverageIds.isEmpty()) {
            return new HashSet<>();
        }

        // Przefiltruj nieprawidłowe ID
        List<Long> validIds = coverageIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            log.warn("Wszystkie ID coverage są nieprawidłowe");
            return new HashSet<>();
        }

        // Jedno zapytanie dla wszystkich ID
        Set<BikeRepairCoverage> foundCoverages = new HashSet<>(bikeRepairCoverageRepository.findAllById(validIds));

        // Waliduj czy znaleziono wszystkie wymagane coverage'y
        if (foundCoverages.size() != validIds.size()) {
            Set<Long> foundIds = foundCoverages.stream()
                    .map(BikeRepairCoverage::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = validIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());

            throw new ValidationException("Nie znaleziono coverage'ów o ID: " + missingIds);
        }

        log.debug("Załadowano {} coverage'ów jednym zapytaniem", foundCoverages.size());
        return foundCoverages;
    }


    private BikeRepairCoverage createNewCoverageWithCategory(BikeRepairCoverageDto dto, BikeRepairCoverageCategory category) {
        log.info("Tworzenie nowego pokrycia napraw: {} dla kategorii: {}", dto.getName(), category.getName());

        BikeRepairCoverage newCoverage = new BikeRepairCoverage();
        newCoverage.setName(dto.getName());
        newCoverage.setCategory(category);
        newCoverage.setCreatedAt(LocalDateTime.now());

        return bikeRepairCoverageRepository.save(newCoverage);
    }

    private void assignCoveragesToService(BikeServiceRegistered bikeService, Set<BikeRepairCoverage> coverages) {
        bikeService.getBikeRepairCoverages().clear();
        for (BikeRepairCoverage coverage : coverages) {
            bikeService.addBikeRepairCoverage(coverage);
        }

        log.info("Przypisano {} pokryć napraw dla serwisu: {} (ID: {})",
                coverages.size(), bikeService.getName(), bikeService.getId());
    }

    private ResponseEntity<?> createSuccessResponse(BikeServiceRegistered bikeService) {
        int finalCount = bikeService.getBikeRepairCoverages().size();
        String message = String.format("Pokrycia napraw zostały pomyślnie zaktualizowane. Aktualnie przypisano: %d", finalCount);
        return ResponseEntity.ok(message);
    }

    // Klasa wyjątku walidacji
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

}
