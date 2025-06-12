package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Serwis do obsługi zamówień serwisowych (transport + serwis)
 */
public interface ServiceOrderService {

    // === TWORZENIE ZAMÓWIEŃ ===

    /**
     * Tworzy zamówienie serwisowe dla zalogowanego użytkownika
     */
    ResponseEntity<?> createServiceOrder(ServiceOrTransportOrderDto dto, String userEmail);

    /**
     * Tworzy zamówienie serwisowe dla gościa
     */
    ResponseEntity<?> createGuestServiceOrder(ServiceOrTransportOrderDto dto);

    // === POBIERANIE ZAMÓWIEŃ ===

    /**
     * Pobiera zamówienia serwisowe użytkownika
     */
    List<UnifiedOrderResponseDto> getUserServiceOrders(String userEmail);

    /**
     * Pobiera szczegóły zamówienia serwisowego
     */
    ResponseEntity<ServiceOrderDetailsResponseDto> getServiceOrderDetails(Long orderId, String userEmail);

    // === AKTUALIZACJA ZAMÓWIEŃ ===

    /**
     * Aktualizuje zamówienie serwisowe
     */
    ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrTransportOrderDto dto, String userEmail);

    // === ADMIN ===

    /**
     * Pobiera wszystkie zamówienia serwisowe (admin)
     */
    List<UnifiedOrderResponseDto> getAllServiceOrders();

    /**
     * Aktualizuje zamówienie serwisowe (admin)
     */
    ResponseEntity<?> updateServiceOrderByAdmin(Long orderId, ServiceOrTransportOrderDto dto, String adminEmail);

    /**
     * Usuwa zamówienie serwisowe (admin)
     */
    ResponseEntity<?> deleteServiceOrder(Long orderId, String adminEmail);

    // === UNIFIED DTO METHODS ===

    /**
     * Pobiera wszystkie zamówienia serwisowe jako UnifiedOrderResponseDto (admin)
     */
    List<UnifiedOrderResponseDto> getAllServiceOrdersAsUnified();

    /**
     * Pobiera zamówienie serwisowe jako UnifiedOrderResponseDto (admin)
     */
    Optional<UnifiedOrderResponseDto> getOrderAsUnified(Long orderId);

    ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail);

}