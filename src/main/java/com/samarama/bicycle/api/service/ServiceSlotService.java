package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServiceSlotAvailabilityDto;
import com.samarama.bicycle.api.dto.ServiceSlotConfigDto;
import com.samarama.bicycle.api.model.ServiceSlotConfig;

import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.List;

/**
 * Interfejs serwisu do zarządzania slotami serwisowymi
 */
public interface ServiceSlotService {

    /**
     * Pobiera wszystkie konfiguracje slotów
     * @return lista wszystkich konfiguracji
     */
    List<ServiceSlotConfigDto> getAllSlotConfigs();

    /**
     * Pobiera aktywne konfiguracje slotów (aktywne w bieżącym momencie)
     * @return lista aktywnych konfiguracji
     */
    List<ServiceSlotConfigDto> getCurrentlyActiveSlotConfigs();

    /**
     * Pobiera przyszłe konfiguracje slotów (startDate jest w przyszłości)
     * @return lista przyszłych konfiguracji
     */
    List<ServiceSlotConfigDto> getFutureSlotConfigs();

    /**
     * Tworzy nową konfigurację slotów
     * @param configDto dane konfiguracji
     * @return odpowiedź HTTP z rezultatem operacji
     */
    ResponseEntity<?> createSlotConfig(ServiceSlotConfigDto configDto);

    /**
     * Aktualizuje istniejącą konfigurację slotów
     * @param id identyfikator konfiguracji
     * @param configDto nowe dane konfiguracji
     * @return odpowiedź HTTP z rezultatem operacji
     */
    ResponseEntity<?> updateSlotConfig(Long id, ServiceSlotConfigDto configDto);

    /**
     * Usuwa konfigurację slotów
     * @param id identyfikator konfiguracji
     * @return odpowiedź HTTP z rezultatem operacji
     */
    ResponseEntity<?> deleteSlotConfig(Long id);

    /**
     * Pobiera dostępność slotów na dany dzień
     * @param date data
     * @return informacja o dostępności
     */
    ServiceSlotAvailabilityDto getSlotAvailability(LocalDate date);

    /**
     * Pobiera dostępność slotów na zakres dat
     * @param startDate data początkowa (włącznie)
     * @param endDate data końcowa (włącznie)
     * @return lista informacji o dostępności
     */
    List<ServiceSlotAvailabilityDto> getSlotAvailability(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera dostępność slotów na następne X dni
     * @param startDate data początkowa (włącznie)
     * @param days liczba dni
     * @return lista informacji o dostępności
     */
    List<ServiceSlotAvailabilityDto> getNextDaysAvailability(LocalDate startDate, int days);

    /**
     * Sprawdza, czy na dany dzień jest dostępna odpowiednia liczba slotów
     * @param date data
     * @param bikesCount liczba rowerów do serwisu
     * @return true, jeśli jest wystarczająca liczba wolnych slotów
     */
    boolean areSlotsAvailable(LocalDate date, int bikesCount);

    /**
     * Sprawdza, czy liczba rowerów nie przekracza maksymalnej liczby rowerów na zamówienie
     * @param date data
     * @param bikesCount liczba rowerów
     * @return true, jeśli liczba rowerów jest akceptowalna
     */
    boolean isWithinMaxBikesPerOrder(LocalDate date, int bikesCount);

    /**
     * Pobiera maksymalną liczbę rowerów na jedno zamówienie dla danej daty
     * @param date data
     * @return maksymalna liczba rowerów
     */
    int getMaxBikesPerOrder(LocalDate date);

    /**
     * Pobiera maksymalną liczbę rowerów na dzień dla danej daty
     * @param date data
     * @return maksymalna liczba rowerów na dzień
     */
    int getMaxBikesPerDay(LocalDate date);

    /**
     * Inicjalizuje domyślną konfigurację slotów, jeśli żadna nie istnieje
     */
    void initializeDefaultSlotConfig();

    int countOrderOnDate(LocalDate date);
}