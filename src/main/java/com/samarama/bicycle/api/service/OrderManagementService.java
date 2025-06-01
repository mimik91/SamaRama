package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.OrderFilterDto;
import com.samarama.bicycle.api.dto.ServiceAndTransportOrdersDto;
import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.TransportOrderDto;
import com.samarama.bicycle.api.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Serwis do zarządzania wszystkimi typami zamówień
 */
public interface OrderManagementService {

    // === ADMIN - ZAMÓWIENIA SERWISOWE ===

    /**
     * Pobiera wszystkie zamówienia serwisowe z filtrowaniem i paginacją (dla admina)
     */
    Page<ServiceAndTransportOrdersDto> getAllServiceOrders(OrderFilterDto filter, Pageable pageable);

    /**
     * Wyszukuje zamówienia serwisowe po email/telefonie klienta
     */
    List<ServiceAndTransportOrdersDto> searchServiceOrders(String searchTerm);

    /**
     * Aktualizuje zamówienie serwisowe (admin)
     */
    ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrderDto dto, String adminEmail);

    /**
     * Usuwa zamówienie serwisowe (admin)
     */
    ResponseEntity<?> deleteServiceOrder(Long orderId, String adminEmail);

    /**
     * Zmienia status zamówienia serwisowego (admin)
     */
    ResponseEntity<?> updateServiceOrderStatus(Long orderId, String newStatus, String adminEmail);

    // === ADMIN - ZAMÓWIENIA TRANSPORTOWE ===

    /**
     * Pobiera wszystkie zamówienia transportowe z filtrowaniem i paginacją (dla admina)
     */
    Page<ServiceAndTransportOrdersDto> getAllTransportOrders(OrderFilterDto filter, Pageable pageable);

    /**
     * Wyszukuje zamówienia transportowe po email/telefonie klienta
     */
    List<ServiceAndTransportOrdersDto> searchTransportOrders(String searchTerm);

    /**
     * Aktualizuje zamówienie transportowe (admin)
     */
    ResponseEntity<?> updateTransportOrder(Long orderId, TransportOrderDto dto, String adminEmail);

    /**
     * Usuwa zamówienie transportowe (admin)
     */
    ResponseEntity<?> deleteTransportOrder(Long orderId, String adminEmail);

    /**
     * Zmienia status zamówienia transportowego (admin)
     */
    ResponseEntity<?> updateTransportOrderStatus(Long orderId, String newStatus, String adminEmail);

    // === ADMIN - WSZYSTKIE ZAMÓWIENIA ===

    /**
     * Pobiera wszystkie zamówienia (serwisowe + transportowe) z niezbędnymi danymi
     */
    Page<ServiceAndTransportOrdersDto> getAllOrders(OrderFilterDto filter, Pageable pageable);

    /**
     * Wyszukuje wszystkie zamówienia po email/telefonie klienta
     */
    List<ServiceAndTransportOrdersDto> searchAllOrders(String searchTerm);

    // === KLIENT - SWOJE ZAMÓWIENIA ===

    /**
     * Pobiera zamówienia serwisowe klienta
     */
    List<ServiceAndTransportOrdersDto> getUserServiceOrders(String userEmail);

    /**
     * Pobiera zamówienia transportowe klienta
     */
    List<ServiceAndTransportOrdersDto> getUserTransportOrders(String userEmail);

    /**
     * Pobiera wszystkie zamówienia klienta
     */
    List<ServiceAndTransportOrdersDto> getUserAllOrders(String userEmail);

    /**
     * Aktualizuje zamówienie serwisowe (klient - tylko PENDING/CONFIRMED)
     */
    ResponseEntity<?> updateUserServiceOrder(Long orderId, ServiceOrderDto dto, String userEmail);

    /**
     * Aktualizuje zamówienie transportowe (klient - tylko PENDING/CONFIRMED)
     */
    ResponseEntity<?> updateUserTransportOrder(Long orderId, TransportOrderDto dto, String userEmail);

    /**
     * Anuluje zamówienie (klient - tylko PENDING/CONFIRMED)
     */
    ResponseEntity<?> cancelUserOrder(Long orderId, String userEmail);

    // === POMOCNICZE ===

    /**
     * Pobiera szczegóły zamówienia (admin lub właściciel)
     */
    ResponseEntity<ServiceAndTransportOrdersDto> getOrderDetails(Long orderId, String userEmail, boolean isAdmin);

    /**
     * Sprawdza czy zamówienie można modyfikować
     */
    boolean canModifyOrder(Order order, boolean isAdmin);

    // Dodaj te metody do interfejsu OrderManagementService.java

// === TRANSPORT ORDER CREATION ===

    /**
     * Tworzy nowe zamówienie transportowe (dla zalogowanych użytkowników)
     */
    ResponseEntity<?> createTransportOrder(TransportOrderDto dto, String userEmail);

    /**
     * Tworzy zamówienie transportowe dla gości
     */
    ResponseEntity<?> createGuestTransportOrder(GuestTransportOrderDto dto);

// === SERVICE ORDER CREATION ===

    /**
     * Tworzy nowe zamówienie serwisowe (dla zalogowanych użytkowników)
     */
    ResponseEntity<?> createServiceOrder(ServiceOrderDto dto, String userEmail);

// === STATUS UPDATES ===

    /**
     * Aktualizuje status zamówienia serwisowego (klient)
     */
    ResponseEntity<?> updateServiceOrderStatus(Long orderId, String newStatus, String userEmail);

    /**
     * Aktualizuje status zamówienia transportowego (klient)
     */
    ResponseEntity<?> updateTransportOrderStatus(Long orderId, String newStatus, String userEmail);
}