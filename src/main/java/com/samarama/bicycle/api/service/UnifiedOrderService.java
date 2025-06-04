package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Ujednolicony serwis do obsługi wszystkich typów zamówień
 * Centralizuje logikę biznesową z UnifiedOrderController
 */
public interface UnifiedOrderService {

    // === PUBLICZNE METODY ===

    /**
     * Pobiera dostępne serwisy rowerowe
     */
    ResponseEntity<List<BikeServicePinDto>> getAvailableServices();

    /**
     * Pobiera dostępne pakiety serwisowe
     */
    ResponseEntity<List<ServicePackageDto>> getServicePackages();

    /**
     * Pobiera szczegóły serwisu
     */
    ResponseEntity<BikeServiceDto> getServiceDetails(Long serviceId);

    /**
     * Sprawdza dostępność slotów
     */
    ResponseEntity<?> checkAvailability(String date, int bikesCount);

    /**
     * Oblicza koszt transportu
     */
    ResponseEntity<?> calculateTransportCost(Map<String, Object> request);

    /**
     * Pobiera informacje o zasadach i statusach
     */
    ResponseEntity<?> getOrderInfo();

    // === METODY DLA UŻYTKOWNIKÓW ===

    /**
     * Pobiera adresy użytkownika
     */
    ResponseEntity<List<AddressDto>> getUserAddresses(String userEmail);

    /**
     * Pobiera szczegóły zamówienia użytkownika
     */
    ResponseEntity<UnifiedOrderResponseDto> getUserOrderDetails(Long orderId, String userEmail);

    /**
     * Tworzy zamówienie dla użytkownika
     */
    ResponseEntity<?> createUserOrder(ServiceOrTransportOrderDto dto, String userEmail);

    /**
     * Anuluje zamówienie użytkownika
     */
    ResponseEntity<?> cancelUserOrder(Long orderId, String userEmail);

    /**
     * Pobiera statystyki użytkownika
     */
    ResponseEntity<?> getUserStatistics(String userEmail);

    // === METODY DLA GOŚCI ===

    /**
     * Tworzy zamówienie dla gościa
     */
    ResponseEntity<?> createGuestOrder(ServiceOrTransportOrderDto dto);

    // === METODY ADMINISTRACYJNE ===

    /**
     * Pobiera wszystkie zamówienia (admin)
     */
    ResponseEntity<List<UnifiedOrderResponseDto>> getAllOrdersForAdmin();

    /**
     * Wyszukuje zamówienia (admin)
     */
    ResponseEntity<List<UnifiedOrderResponseDto>> searchOrders(String searchTerm);

    /**
     * Aktualizuje status zamówienia (admin)
     */
    ResponseEntity<?> updateOrderStatus(Long orderId, String newStatus, String adminEmail);

    // === METODY SERWISOWE ===

    /**
     * Aktualizuje notatki serwisowe
     */
    ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail);
}