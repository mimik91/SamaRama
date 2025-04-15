package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServicePackageDto;
import com.samarama.bicycle.api.model.ServicePackage;
import com.samarama.bicycle.api.repository.ServicePackageRepository;
import com.samarama.bicycle.api.service.ServicePackageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ServicePackageServiceImpl implements ServicePackageService {

    private static final Logger logger = Logger.getLogger(ServicePackageServiceImpl.class.getName());

    private final ServicePackageRepository servicePackageRepository;

    @Autowired
    public ServicePackageServiceImpl(ServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    @PostConstruct
    public void init() {
        initializeDefaultServicePackages();
    }

    @Override
    public List<ServicePackage> getAllServicePackages() {
        return servicePackageRepository.findAll();
    }

    @Override
    public List<ServicePackage> getActiveServicePackages() {
        // Pobierz aktywne pakiety posortowane według kolejności wyświetlania (jeśli jest ustawiona)
        List<ServicePackage> packages = servicePackageRepository.findByActiveTrueOrderByDisplayOrderAsc();

        // Jeśli nie ma ustawionej kolejności, posortuj według ceny (od najtańszego)
        if (packages.stream().allMatch(p -> p.getDisplayOrder() == null)) {
            packages.sort(Comparator.comparing(ServicePackage::getPrice));
        }

        return packages;
    }

    @Override
    public Optional<ServicePackage> getServicePackageById(Long id) {
        return servicePackageRepository.findById(id);
    }

    @Override
    public Optional<ServicePackage> getServicePackageByCode(String code) {
        return servicePackageRepository.findByCode(code);
    }

    @Override
    @Transactional
    public ResponseEntity<?> createServicePackage(ServicePackageDto dto) {
        // Sprawdź, czy pakiet o podanym kodzie już istnieje
        if (servicePackageRepository.existsByCode(dto.code())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Pakiet serwisowy o kodzie '" + dto.code() + "' już istnieje"));
        }

        try {
            ServicePackage servicePackage = new ServicePackage();
            servicePackage.setCode(dto.code());
            servicePackage.setName(dto.name());
            servicePackage.setDescription(dto.description());
            servicePackage.setPrice(dto.price());
            servicePackage.setActive(dto.active() != null ? dto.active() : true);
            servicePackage.setDisplayOrder(dto.displayOrder());

            if (dto.features() != null) {
                servicePackage.setFeatures(new ArrayList<>(dto.features()));
            }

            ServicePackage savedPackage = servicePackageRepository.save(servicePackage);

            return ResponseEntity.ok(Map.of(
                    "message", "Pakiet serwisowy został utworzony pomyślnie",
                    "id", savedPackage.getId()
            ));
        } catch (Exception e) {
            logger.severe("Błąd podczas tworzenia pakietu serwisowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas tworzenia pakietu serwisowego: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServicePackage(Long id, ServicePackageDto dto) {
        Optional<ServicePackage> packageOpt = servicePackageRepository.findById(id);

        if (packageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Sprawdź, czy kod pakietu nie koliduje z innym pakietem
        if (!dto.code().equals(packageOpt.get().getCode()) && servicePackageRepository.existsByCode(dto.code())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Pakiet serwisowy o kodzie '" + dto.code() + "' już istnieje"));
        }

        try {
            ServicePackage servicePackage = packageOpt.get();
            servicePackage.setCode(dto.code());
            servicePackage.setName(dto.name());
            servicePackage.setDescription(dto.description());
            servicePackage.setPrice(dto.price());
            servicePackage.setActive(dto.active() != null ? dto.active() : servicePackage.isActive());
            servicePackage.setDisplayOrder(dto.displayOrder());

            if (dto.features() != null) {
                servicePackage.setFeatures(new ArrayList<>(dto.features()));
            }

            servicePackageRepository.save(servicePackage);

            return ResponseEntity.ok(Map.of("message", "Pakiet serwisowy został zaktualizowany pomyślnie"));
        } catch (Exception e) {
            logger.severe("Błąd podczas aktualizacji pakietu serwisowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas aktualizacji pakietu serwisowego: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteServicePackage(Long id) {
        Optional<ServicePackage> packageOpt = servicePackageRepository.findById(id);

        if (packageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            servicePackageRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Pakiet serwisowy został usunięty pomyślnie"));
        } catch (Exception e) {
            logger.severe("Błąd podczas usuwania pakietu serwisowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas usuwania pakietu serwisowego: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> toggleServicePackageActive(Long id, boolean active) {
        Optional<ServicePackage> packageOpt = servicePackageRepository.findById(id);

        if (packageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            ServicePackage servicePackage = packageOpt.get();
            servicePackage.setActive(active);
            servicePackageRepository.save(servicePackage);

            String status = active ? "aktywowany" : "dezaktywowany";
            return ResponseEntity.ok(Map.of("message", "Pakiet serwisowy został " + status + " pomyślnie"));
        } catch (Exception e) {
            logger.severe("Błąd podczas zmiany statusu pakietu serwisowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas zmiany statusu pakietu serwisowego: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public void initializeDefaultServicePackages() {
        // Sprawdź, czy istnieją już jakieś pakiety
        if (servicePackageRepository.count() > 0) {
            logger.info("Pakiety serwisowe już istnieją w bazie danych - pomijam inicjalizację");
            return;
        }

        logger.info("Inicjalizacja domyślnych pakietów serwisowych");

        // Pakiet BASIC
        ServicePackage basicPackage = new ServicePackage();
        basicPackage.setCode("BASIC");
        basicPackage.setName("Przegląd podstawowy");
        basicPackage.setDescription("Podstawowe sprawdzenie stanu roweru i regulacje");
        basicPackage.setPrice(new BigDecimal("200.00"));
        basicPackage.setActive(true);
        basicPackage.setDisplayOrder(1);
        basicPackage.setFeatures(List.of(
                "Ocena stanu technicznego roweru",
                "Regulacja hamulców",
                "Regulacja przerzutek",
                "Smarowanie łańcucha",
                "Sprawdzenie ciśnienia w ogumieniu",
                "Sprawdzenie poprawności skręcenia roweru",
                "Kontrola luzu sterów",
                "Kontrola połączeń śrubowych",
                "Sprawdzenie linek, pancerzy",
                "Sprawdzenie stanu opon",
                "Kasowanie luzów i regulacja elementów ruchomych"
        ));

        // Pakiet EXTENDED
        ServicePackage extendedPackage = new ServicePackage();
        extendedPackage.setCode("EXTENDED");
        extendedPackage.setName("Przegląd rozszerzony");
        extendedPackage.setDescription("Rozszerzony przegląd z czyszczeniem i wymianą podstawowych części");
        extendedPackage.setPrice(new BigDecimal("350.00"));
        extendedPackage.setActive(true);
        extendedPackage.setDisplayOrder(2);
        extendedPackage.setFeatures(List.of(
                "Wszystkie elementy przeglądu podstawowego",
                "Czyszczenie i smarowanie łańcucha, kasety",
                "Wymiana smaru w sterach, piastach, suporcie",
                "Kontrola kół",
                "Kontrola działania amortyzatora",
                "W cenie wymiana klocków, linek, pancerzy, dętek, opon, łańcucha, kasety lub wolnobiegu. Do ceny należy doliczyć koszt części, które wymagają wymiany."
        ));

        // Pakiet FULL
        ServicePackage fullPackage = new ServicePackage();
        fullPackage.setCode("FULL");
        fullPackage.setName("Przegląd pełny");
        fullPackage.setDescription("Kompleksowy przegląd i konserwacja całego roweru");
        fullPackage.setPrice(new BigDecimal("600.00"));
        fullPackage.setActive(true);
        fullPackage.setDisplayOrder(3);
        fullPackage.setFeatures(List.of(
                "Wszystkie elementy przeglądu rozszerzonego",
                "Mycie roweru",
                "Czyszczenie i konserwacja przerzutek",
                "Czyszczenie i smarowanie łańcucha, kasety, korby",
                "Wymiana smaru w sterach, piastach, suporcie",
                "Wymiana linek i pancerzy",
                "Kontrola luzu łożysk suportu, steru, piast",
                "Sprawdzenie połączeń gwintowych",
                "Zewnętrzna konserwacja goleni amortyzatora",
                "Centrowanie kół",
                "Linki i pancerze oraz mycie roweru są wliczone w cenę przeglądu"
        ));

        // Zapisanie pakietów do bazy danych
        servicePackageRepository.saveAll(List.of(basicPackage, extendedPackage, fullPackage));

        logger.info("Pomyślnie zainicjalizowano domyślne pakiety serwisowe");
    }
}