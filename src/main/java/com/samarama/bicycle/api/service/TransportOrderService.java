package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.TransportOrder;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
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
    ResponseEntity<?> createTransportOrder(ServiceOrTransportOrderDto dto, String userEmail);

    /**
     * Tworzy zamówienie transportowe dla gościa
     */
    ResponseEntity<?> createGuestTransportOrder(ServiceOrTransportOrderDto dto);

    // === POBIERANIE ZAMÓWIEŃ ===


    /**
     * Pobiera wszystkie zamówienia (transport + serwis) użytkownika
     */
    List<ServiceOrderResponseDto> getAllUserOrders(String userEmail);

    /**
     * Pobiera szczegóły zamówienia
     */
    ResponseEntity<ServiceOrderDetailsResponseDto> getOrderDetails(Long orderId, String userEmail);

    // === AKTUALIZACJA ZAMÓWIEŃ ===

    /**
     * Aktualizuje zamówienie transportowe
     */
    ResponseEntity<?> updateTransportOrder(Long orderId, ServiceOrTransportOrderDto dto, String userEmail);

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
    ResponseEntity<?> updateTransportOrderByAdmin(Long orderId, ServiceOrTransportOrderDto dto, String adminEmail);

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

    List<CourierOrderDto> getCourierOrders();

    BigDecimal checkDiscount(String coupon, BigDecimal bigDecimal, LocalDate localDate);

    List<TransportOrderDto> getOrdersByIds(@NotEmpty(message = "Order IDs list cannot be empty") List<Long> longs);
}