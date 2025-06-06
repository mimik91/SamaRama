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



    /**
     * Oblicza koszt transportu
     */
    ResponseEntity<?> calculateTransportCost(Map<String, Object> request);


    /**
     * Tworzy zamówienie dla użytkownika
     */
    ResponseEntity<?> createUserOrder(ServiceOrTransportOrderDto dto, String userEmail);

    /**
     * Anuluje zamówienie użytkownika
     */
    ResponseEntity<?> cancelUserOrder(Long orderId, String userEmail);

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