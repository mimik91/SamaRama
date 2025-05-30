package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminOrderController {

    private final OrderManagementService orderManagementService;

    public AdminOrderController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    // === ZAMÓWIENIA SERWISOWE ===

    /**
     * Pobiera wszystkie zamówienia serwisowe z filtrowaniem i paginacją
     * Query params:
     * - pickupDateFrom, pickupDateTo (format: YYYY-MM-DD)
     * - status (PENDING, CONFIRMED, PICKED_UP, IN_SERVICE, COMPLETED, DELIVERED, CANCELLED)
     * - searchTerm (email lub telefon klienta)
     * - servicePackageCode, servicePackageId
     * - sortBy (orderDate, pickupDate, status, client)
     * - sortOrder (ASC, DESC)
     * - page, size (paginacja)
     */
    @GetMapping("/service")
    public ResponseEntity<Page<ServiceAndTransportOrdersDto>> getAllServiceOrders(
            OrderFilterDto filter,
            @PageableDefault(size = 20, sort = "orderDate") Pageable pageable) {

        Page<ServiceAndTransportOrdersDto> orders = orderManagementService.getAllServiceOrders(filter, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Wyszukuje zamówienia serwisowe po email/telefonie klienta
     */
    @GetMapping("/service/search")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> searchServiceOrders(
            @RequestParam String searchTerm) {

        List<ServiceAndTransportOrdersDto> orders = orderManagementService.searchServiceOrders(searchTerm);
        return ResponseEntity.ok(orders);
    }

    /**
     * Aktualizuje zamówienie serwisowe
     */
    @PutMapping("/service/{orderId}")
    public ResponseEntity<?> updateServiceOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ServiceOrderDto serviceOrderDto) {

        String adminEmail = getCurrentUserEmail();
        return orderManagementService.updateServiceOrder(orderId, serviceOrderDto, adminEmail);
    }

    /**
     * Usuwa zamówienie serwisowe
     */
    @DeleteMapping("/service/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteServiceOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return orderManagementService.deleteServiceOrder(orderId, adminEmail);
    }

    /**
     * Zmienia status zamówienia serwisowego
     */
    @PatchMapping("/service/{orderId}/status")
    public ResponseEntity<?> updateServiceOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        String adminEmail = getCurrentUserEmail();
        return orderManagementService.updateServiceOrderStatus(orderId, newStatus, adminEmail);
    }

    // === ZAMÓWIENIA TRANSPORTOWE ===

    /**
     * Pobiera wszystkie zamówienia transportowe z filtrowaniem i paginacją
     */
    @GetMapping("/transport")
    public ResponseEntity<Page<ServiceAndTransportOrdersDto>> getAllTransportOrders(
            OrderFilterDto filter,
            @PageableDefault(size = 20, sort = "orderDate") Pageable pageable) {

        Page<ServiceAndTransportOrdersDto> orders = orderManagementService.getAllTransportOrders(filter, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Wyszukuje zamówienia transportowe po email/telefonie klienta
     */
    @GetMapping("/transport/search")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> searchTransportOrders(
            @RequestParam String searchTerm) {

        List<ServiceAndTransportOrdersDto> orders = orderManagementService.searchTransportOrders(searchTerm);
        return ResponseEntity.ok(orders);
    }

    /**
     * Aktualizuje zamówienie transportowe
     */
    @PutMapping("/transport/{orderId}")
    public ResponseEntity<?> updateTransportOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody TransportOrderDto transportOrderDto) {

        String adminEmail = getCurrentUserEmail();
        return orderManagementService.updateTransportOrder(orderId, transportOrderDto, adminEmail);
    }

    /**
     * Usuwa zamówienie transportowe
     */
    @DeleteMapping("/transport/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTransportOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return orderManagementService.deleteTransportOrder(orderId, adminEmail);
    }

    /**
     * Zmienia status zamówienia transportowego lub status transportu
     */
    @PatchMapping("/transport/{orderId}/status")
    public ResponseEntity<?> updateTransportOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        String adminEmail = getCurrentUserEmail();
        return orderManagementService.updateTransportOrderStatus(orderId, newStatus, adminEmail);
    }

    // === WSZYSTKIE ZAMÓWIENIA ===

    /**
     * Pobiera wszystkie zamówienia (serwisowe + transportowe) z niezbędnymi danymi do wykonania
     * Zawiera: adres odbioru, adres dostarczenia, datę odbioru, status
     */
    @GetMapping("/all")
    public ResponseEntity<Page<ServiceAndTransportOrdersDto>> getAllOrders(
            OrderFilterDto filter,
            @PageableDefault(size = 20, sort = "orderDate") Pageable pageable) {

        Page<ServiceAndTransportOrdersDto> orders = orderManagementService.getAllOrders(filter, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Wyszukuje wszystkie zamówienia po email/telefonie klienta
     */
    @GetMapping("/all/search")
    public ResponseEntity<List<ServiceAndTransportOrdersDto>> searchAllOrders(
            @RequestParam String searchTerm) {

        List<ServiceAndTransportOrdersDto> orders = orderManagementService.searchAllOrders(searchTerm);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera szczegóły konkretnego zamówienia
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ServiceAndTransportOrdersDto> getOrderDetails(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return orderManagementService.getOrderDetails(orderId, adminEmail, true);
    }

    // === STATYSTYKI I RAPORTY ===

    /**
     * Pobiera statystyki zamówień
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        // Możesz rozszerzyć to o dodatkowe statystyki
        Map<String, Object> stats = Map.of(
                "message", "Statystyki będą dostępne w przyszłej wersji",
                "totalServiceOrders", orderManagementService.getAllServiceOrders(OrderFilterDto.empty(), Pageable.unpaged()).getTotalElements(),
                "totalTransportOrders", orderManagementService.getAllTransportOrders(OrderFilterDto.empty(), Pageable.unpaged()).getTotalElements()
        );

        return ResponseEntity.ok(stats);
    }

    // === HELPER METHODS ===

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}