package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServicePinDto;
import com.samarama.bicycle.api.model.BicycleEnumeration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Interfejs serwisu do obsługi serwisów rowerowych
 */
public interface BikeServiceService {

    // === PUBLICZNE METODY ===

    /**
     * Pobierz piny wszystkich serwisów rowerowych (tylko ID i współrzędne)
     * @return lista pinów serwisów które mają współrzędne geograficzne
     */
    List<BikeServicePinDto> getAllBikeServicePins();

    /**
     * Pobierz szczegóły serwisu rowerowego po ID
     * @param id identyfikator serwisu
     * @return pełne informacje o serwisie lub Optional.empty() jeśli nie znaleziono
     */
    Optional<BikeServiceDto> getBikeServiceDetails(Long id);

    // === METODY ADMINISTRACYJNE ===

    /**
     * Pobierz wszystkie serwisy rowerowe (dla administratora)
     * @return lista wszystkich serwisów z pełnymi informacjami
     */
    List<BikeServiceDto> getAllBikeServicesForAdmin();

    /**
     * Utwórz nowy serwis rowerowy
     * @param bikeServiceDto dane nowego serwisu
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> createBikeService(BikeServiceDto bikeServiceDto);

    /**
     * Zaktualizuj istniejący serwis rowerowy
     * @param id identyfikator serwisu
     * @param bikeServiceDto zaktualizowane dane serwisu
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> updateBikeService(Long id, BikeServiceDto bikeServiceDto);

    /**
     * Usuń serwis rowerowy
     * @param id identyfikator serwisu
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> deleteBikeService(Long id);

    /**
     * Import serwisów z pliku CSV
     * @param file plik CSV z danymi serwisów
     * @param adminEmail email administratora wykonującego import
     * @return odpowiedź z wynikiem importu
     */
    ResponseEntity<?> importBikeServicesFromCsv(MultipartFile file, String adminEmail);

    /**
     * Pobierz statystyki serwisów
     * @return mapa ze statystykami
     */
    ResponseEntity<?> getBikeServiceStatistics();

    void updateTransportPrices(String previous, String newPrice);
}