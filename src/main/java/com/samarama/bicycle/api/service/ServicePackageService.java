package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServicePackageDto;
import com.samarama.bicycle.api.model.ServicePackage;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interfejs serwisu do obsługi pakietów serwisowych
 */
public interface ServicePackageService {

    /**
     * Pobierz wszystkie pakiety serwisowe
     */
    List<ServicePackage> getAllServicePackages();

    /**
     * Pobierz tylko aktywne pakiety serwisowe
     */
    List<ServicePackage> getActiveServicePackages();

    /**
     * Pobierz pakiet serwisowy po identyfikatorze
     */
    Optional<ServicePackage> getServicePackageById(Long id);

    /**
     * Pobierz pakiet serwisowy po kodzie
     */
    Optional<ServicePackage> getServicePackageByCode(String code);

    /**
     * Zapisz nowy pakiet serwisowy
     */
    ResponseEntity<?> createServicePackage(ServicePackageDto servicePackageDto);

    /**
     * Zaktualizuj istniejący pakiet serwisowy
     */
    ResponseEntity<?> updateServicePackage(Long id, ServicePackageDto servicePackageDto);

    /**
     * Usuń pakiet serwisowy (fizyczne usunięcie z bazy danych)
     */
    ResponseEntity<?> deleteServicePackage(Long id);

    /**
     * Zmień status aktywności pakietu serwisowego
     */
    ResponseEntity<?> toggleServicePackageActive(Long id, boolean active);

    /**
     * Zainicjalizuj domyślne pakiety serwisowe, jeśli nie istnieją
     */
    void initializeDefaultServicePackages();
}