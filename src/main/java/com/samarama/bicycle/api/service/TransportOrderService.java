package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Serwis do obsługi zamówień transportowych (bazowych)
 */
public interface TransportOrderService {

    // === TWORZENIE ZAMÓWIEŃ ===

    /**
     * Tworzy zamówienie transportowe dla zalogowanego użytkownika
     */
    ResponseEntity<?> createTransportOrder(TransportOrderDto dto, String userEmail);

    /**
     * Tworzy zamówienie transportowe dla gościa
     */
    ResponseEntity<?> createGuestTransportOrder(TransportOrderDto dto);

    // === POBIERANIE ZAMÓWIEŃ ===

    /**
     * Pobiera zamówienia transportowe użytkownika (TYLKO czyste transport)
     */
    List<UnifiedOrderResponseDto> getUserTransportOrders(String userEmail);

    /**
     * Pobiera wszystkie zamówienia (transport + serwis) użytkownika
     */
    List<UnifiedOrderResponseDto> getAllUserOrders(String userEmail);

    /**
     * Pobiera szczegóły zamówienia
     */
    ResponseEntity<UnifiedOrderResponseDto> getOrderDetails(Long orderId, String userEmail);

    // === AKTUALIZACJA ZAMÓWIEŃ ===

    /**
     * Aktualizuje zamówienie transportowe
     */
    ResponseEntity<?> updateTransportOrder(Long orderId, TransportOrderDto dto, String userEmail);

    /**
     * Anuluje zamówienie
     */
    ResponseEntity<?> cancelOrder(Long orderId, String userEmail);

    /**
     * Aktualizuje status zamówienia
     */
    ResponseEntity<?> updateOrderStatus(Long orderId, String newStatus, String userEmail);

    // === ADMIN ===

    /**
     * Pobiera wszystkie zamówienia transportowe (admin)
     */
    List<UnifiedOrderResponseDto> getAllTransportOrders();

    /**
     * Pobiera wszystkie zamówienia (transport + serwis) (admin)
     */
    List<UnifiedOrderResponseDto> getAllOrders();

    /**
     * Wyszukuje zamówienia po email/telefonie klienta
     */
    List<UnifiedOrderResponseDto> searchOrders(String searchTerm);

    /**
     * Aktualizuje zamówienie (admin)
     */
    ResponseEntity<?> updateTransportOrderByAdmin(Long orderId, TransportOrderDto dto, String adminEmail);

    /**
     * Usuwa zamówienie (admin)
     */
    ResponseEntity<?> deleteTransportOrder(Long orderId, String adminEmail);

    /**
     * Aktualizuje status zamówienia (admin)
     */
    ResponseEntity<?> updateOrderStatusByAdmin(Long orderId, String newStatus, String adminEmail);

    // === STATYSTYKI ===

    /**
     * Zlicza zamówienia na dzień
     */
    int countOrdersForDate(LocalDate date);

    /**
     * Sprawdza dostępność slotów
     */
    boolean areSlotsAvailable(LocalDate date, int ordersCount);

    // Dodaj te metody do interfejsu TransportOrderService

    /**
     * Pobiera wszystkie zamówienia transportowe jako UnifiedOrderResponseDto (admin)
     */
    List<UnifiedOrderResponseDto> getAllTransportOrdersAsUnified();

    /**
     * Pobiera zamówienie jako UnifiedOrderResponseDto (admin)
     */
    Optional<UnifiedOrderResponseDto> getOrderAsUnified(Long orderId);
}