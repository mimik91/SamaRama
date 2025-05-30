package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CLIENT')")
public class ClientOrderController {

    private final OrderManagementService orderManagementService;

    public ClientOrderController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    // === POBIERANIE ZAMÓWIEŃ KLIENTA ===

    /**
     * Pobiera wszystkie zamówienia serwisowe klienta
     */
    @GetMapping("/service")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> getUserServiceOrders() {
        String userEmail = getCurrentUserEmail();
        List<ServiceAndTransportOrdersDto> orders = orderManagementService.getUserServiceOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera wszystkie zamówienia transportowe klienta
     */
    @GetMapping("/transport")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> getUserTransportOrders() {
        String userEmail = getCurrentUserEmail();
        List<ServiceAndTransportOrdersDto> orders = orderManagementService.getUserTransportOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera wszystkie zamówienia klienta (serwisowe + transportowe)
     */
    @GetMapping("/all")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> getUserAllOrders() {
        String userEmail = getCurrentUserEmail();
        List<ServiceAndTransportOrdersDto> orders = orderManagementService.getUserAllOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera szczegóły konkretnego zamówienia klienta
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ServiceAndTransportOrdersDto> getOrderDetails(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.getOrderDetails(orderId, userEmail, false);
    }

    // === MODYFIKACJA ZAMÓWIEŃ KLIENTA ===
    // Uwaga: Klient może modyfikować tylko zamówienia w statusie PENDING lub CONFIRMED

    /**
     * Aktualizuje zamówienie serwisowe klienta
     * Można modyfikować tylko w statusie PENDING lub CONFIRMED
     */
    @PutMapping("/service/{orderId}")
    public ResponseEntity<?> updateUserServiceOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ServiceOrderDto serviceOrderDto) {

        String userEmail = getCurrentUserEmail();
        return orderManagementService.updateUserServiceOrder(orderId, serviceOrderDto, userEmail);
    }

    /**
     * Aktualizuje zamówienie transportowe klienta
     * Można modyfikować tylko w statusie PENDING lub CONFIRMED
     */
    @PutMapping("/transport/{orderId}")
    public ResponseEntity<?> updateUserTransportOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody TransportOrderDto transportOrderDto) {

        String userEmail = getCurrentUserEmail();
        return orderManagementService.updateUserTransportOrder(orderId, transportOrderDto, userEmail);
    }

    /**
     * Anuluje zamówienie klienta (ustawia status na CANCELLED)
     * Można anulować tylko w statusie PENDING lub CONFIRMED
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelUserOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.cancelUserOrder(orderId, userEmail);
    }

    // === TWORZENIE NOWYCH ZAMÓWIEŃ ===
    // Te endpointy mogą pozostać w oryginalnych kontrolerach lub można je przenieść tutaj

    /**
     * Informacje o ograniczeniach modyfikacji zamówień
     */
    @GetMapping("/modification-rules")
    public ResponseEntity<?> getModificationRules() {
        return ResponseEntity.ok(Map.of(
                "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "COMPLETED", "DELIVERED", "CANCELLED"),
                "message", "Zamówienia można modyfikować lub anulować tylko w statusie PENDING lub CONFIRMED"
        ));
    }

    // === HELPER METHODS ===

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}