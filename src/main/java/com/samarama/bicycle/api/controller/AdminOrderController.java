package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminOrderController {

    private final TransportOrderService transportOrderService;
    private final ServiceOrderService serviceOrderService;
    private final ServiceSlotService serviceSlotService;

    public AdminOrderController(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            ServiceSlotService serviceSlotService) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.serviceSlotService = serviceSlotService;
    }

    // === POBIERANIE ZAMÓWIEŃ ===

    /**
     * Pobiera wszystkie zamówienia (transport + serwis)
     */
    @GetMapping
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllOrders() {
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia transportowe
     */
    @GetMapping("/transport")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getTransportOrders() {
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllTransportOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia serwisowe
     */
    @GetMapping("/service")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getServiceOrders() {
        List<UnifiedOrderResponseDto> orders = serviceOrderService.getAllServiceOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera szczegóły zamówienia
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<UnifiedOrderResponseDto> getOrderDetails(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();

        // Próbuj jako transport order
        ResponseEntity<UnifiedOrderResponseDto> transportResponse =
                transportOrderService.getOrderDetails(orderId, adminEmail);

        if (transportResponse.getStatusCode().is2xxSuccessful()) {
            return transportResponse;
        }

        // Próbuj jako service order
        return serviceOrderService.getServiceOrderDetails(orderId, adminEmail);
    }

    // === WYSZUKIWANIE I FILTROWANIE ===

    /**
     * Wyszukuje zamówienia po email/telefonie klienta
     */
    @GetMapping("/search")
    public ResponseEntity<List<UnifiedOrderResponseDto>> searchOrders(@RequestParam String searchTerm) {
        List<UnifiedOrderResponseDto> orders = transportOrderService.searchOrders(searchTerm);
        return ResponseEntity.ok(orders);
    }

    /**
     * Filtruje zamówienia według różnych kryteriów
     */
    @GetMapping("/filter")
    public ResponseEntity<List<UnifiedOrderResponseDto>> filterOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String pickupDateFrom,
            @RequestParam(required = false) String pickupDateTo,
            @RequestParam(required = false) String searchTerm) {

        // Pobierz wszystkie zamówienia
        List<UnifiedOrderResponseDto> allOrders = transportOrderService.getAllOrders();

        // Aplikuj filtry
        List<UnifiedOrderResponseDto> filteredOrders = allOrders.stream()
                .filter(order -> status == null || status.equals(order.status()))
                .filter(order -> orderType == null || orderType.equals(order.orderType()))
                .filter(order -> pickupDateFrom == null ||
                        !order.pickupDate().isBefore(LocalDate.parse(pickupDateFrom)))
                .filter(order -> pickupDateTo == null ||
                        !order.pickupDate().isAfter(LocalDate.parse(pickupDateTo)))
                .filter(order -> searchTerm == null ||
                        order.clientEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        (order.clientPhone() != null && order.clientPhone().contains(searchTerm)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredOrders);
    }

    // === ZARZĄDZANIE STATUSEM ===

    /**
     * Zmienia status zamówienia
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, newStatus, adminEmail);
    }

    /**
     * Potwierdza zamówienie
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, "CONFIRMED", adminEmail);
    }

    /**
     * Oznacza zamówienie jako odebrane
     */
    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<?> markAsPickedUp(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, "PICKED_UP", adminEmail);
    }

    /**
     * Oznacza zamówienie jako w drodze powrotnej
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> markAsReturning(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, "ON_THE_WAY_BACK", adminEmail);
    }

    /**
     * Anuluje zamówienie
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, "CANCELLED", adminEmail);
    }

    // === OPERACJE SERWISOWE ===

    /**
     * Rozpoczyna serwis (tylko dla ServiceOrder)
     */
    @PostMapping("/{orderId}/start-service")
    public ResponseEntity<?> startService(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return serviceOrderService.startService(orderId, adminEmail);
    }

    /**
     * Kończy serwis (tylko dla ServiceOrder)
     */
    @PostMapping("/{orderId}/complete-service")
    public ResponseEntity<?> completeService(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return serviceOrderService.completeService(orderId, adminEmail);
    }

    /**
     * Aktualizuje notatki serwisowe
     */
    @PatchMapping("/{orderId}/service-notes")
    public ResponseEntity<?> updateServiceNotes(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String notes = request.get("notes");
        String adminEmail = getCurrentUserEmail();
        return serviceOrderService.updateServiceNotes(orderId, notes, adminEmail);
    }

    // === AKTUALIZACJA ZAMÓWIEŃ ===

    /**
     * Aktualizuje zamówienie transportowe
     */
    @PutMapping("/transport/{orderId}")
    public ResponseEntity<?> updateTransportOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody TransportOrderDto dto) {

        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateTransportOrderByAdmin(orderId, dto, adminEmail);
    }

    /**
     * Aktualizuje zamówienie serwisowe
     */
    @PutMapping("/service/{orderId}")
    public ResponseEntity<?> updateServiceOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ServiceOrderDto dto) {

        String adminEmail = getCurrentUserEmail();
        return serviceOrderService.updateServiceOrderByAdmin(orderId, dto, adminEmail);
    }

    // === USUWANIE ZAMÓWIEŃ ===

    /**
     * Usuwa zamówienie transportowe
     */
    @DeleteMapping("/transport/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTransportOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return transportOrderService.deleteTransportOrder(orderId, adminEmail);
    }

    /**
     * Usuwa zamówienie serwisowe
     */
    @DeleteMapping("/service/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteServiceOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        return serviceOrderService.deleteServiceOrder(orderId, adminEmail);
    }

    // === STATYSTYKI ===

    /**
     * Pobiera statystyki zamówień
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getOrderStatistics() {
        List<UnifiedOrderResponseDto> allOrders = transportOrderService.getAllOrders();

        long totalOrders = allOrders.size();
        long transportOrders = allOrders.stream().filter(o -> "TRANSPORT".equals(o.orderType())).count();
        long serviceOrders = allOrders.stream().filter(o -> "SERVICE".equals(o.orderType())).count();
        long pendingOrders = allOrders.stream().filter(o -> "PENDING".equals(o.status())).count();
        long confirmedOrders = allOrders.stream().filter(o -> "CONFIRMED".equals(o.status())).count();
        long activeOrders = allOrders.stream().filter(o -> !"CANCELLED".equals(o.status()) && !"ON_THE_WAY_BACK".equals(o.status())).count();

        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "transportOrders", transportOrders,
                "serviceOrders", serviceOrders,
                "pendingOrders", pendingOrders,
                "confirmedOrders", confirmedOrders,
                "activeOrders", activeOrders,
                "ordersByStatus", Map.of(
                        "PENDING", allOrders.stream().filter(o -> "PENDING".equals(o.status())).count(),
                        "CONFIRMED", allOrders.stream().filter(o -> "CONFIRMED".equals(o.status())).count(),
                        "PICKED_UP", allOrders.stream().filter(o -> "PICKED_UP".equals(o.status())).count(),
                        "IN_SERVICE", allOrders.stream().filter(o -> "IN_SERVICE".equals(o.status())).count(),
                        "ON_THE_WAY_BACK", allOrders.stream().filter(o -> "ON_THE_WAY_BACK".equals(o.status())).count(),
                        "CANCELLED", allOrders.stream().filter(o -> "CANCELLED".equals(o.status())).count()
                )
        ));
    }

    /**
     * Pobiera statystyki pakietów serwisowych
     */
    @GetMapping("/service-statistics")
    public ResponseEntity<?> getServiceStatistics() {
        List<Object[]> packageStats = serviceOrderService.getServicePackageStatistics();
        Double avgServiceTime = serviceOrderService.getAverageServiceTime();
        List<Object[]> revenue = serviceOrderService.getServiceRevenue();

        return ResponseEntity.ok(Map.of(
                "packageStatistics", packageStats,
                "averageServiceTimeMinutes", avgServiceTime,
                "revenue", revenue
        ));
    }

    // === ZARZĄDZANIE SLOTAMI ===

    /**
     * Pobiera dostępność slotów na zakres dat
     */
    @GetMapping("/slots/availability")
    public ResponseEntity<?> getSlotAvailability(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<ServiceSlotAvailabilityDto> availability =
                serviceSlotService.getSlotAvailability(start, end);

        return ResponseEntity.ok(availability);
    }

    /**
     * Pobiera konfiguracje slotów
     */
    @GetMapping("/slots/config")
    public ResponseEntity<List<ServiceSlotConfigDto>> getSlotConfigs() {
        List<ServiceSlotConfigDto> configs = serviceSlotService.getAllSlotConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Aktualizuje konfigurację slotów
     */
    @PutMapping("/slots/config/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSlotConfig(
            @PathVariable Long configId,
            @Valid @RequestBody ServiceSlotConfigDto dto) {

        return serviceSlotService.updateSlotConfig(configId, dto);
    }

    /**
     * Tworzy nową konfigurację slotów
     */
    @PostMapping("/slots/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSlotConfig(@Valid @RequestBody ServiceSlotConfigDto dto) {
        return serviceSlotService.createSlotConfig(dto);
    }

    // === RAPORTOWANIE ===

    /**
     * Generuje raport zamówień za okres
     */
    @GetMapping("/report")
    public ResponseEntity<?> generateOrderReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String format) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllOrders().stream()
                .filter(order -> !order.pickupDate().isBefore(start) && !order.pickupDate().isAfter(end))
                .collect(Collectors.toList());

        // Podstawowe statystyki dla raportu
        Map<String, Object> report = Map.of(
                "period", Map.of("startDate", start, "endDate", end),
                "totalOrders", orders.size(),
                "ordersByType", Map.of(
                        "transport", orders.stream().filter(o -> "TRANSPORT".equals(o.orderType())).count(),
                        "service", orders.stream().filter(o -> "SERVICE".equals(o.orderType())).count()
                ),
                "ordersByStatus", orders.stream().collect(
                        Collectors.groupingBy(UnifiedOrderResponseDto::status, Collectors.counting())
                ),
                "orders", orders
        );

        return ResponseEntity.ok(report);
    }

    // === UTILITY METHODS ===

    /**
     * Pobiera dostępne statusy dla zamówień
     */
    @GetMapping("/statuses")
    public ResponseEntity<?> getAvailableStatuses() {
        return ResponseEntity.ok(Map.of(
                "allStatuses", List.of(
                        Map.of("value", "PENDING", "label", "Oczekujące", "color", "orange"),
                        Map.of("value", "CONFIRMED", "label", "Potwierdzone", "color", "blue"),
                        Map.of("value", "PICKED_UP", "label", "Odebrane", "color", "purple"),
                        Map.of("value", "IN_SERVICE", "label", "W serwisie", "color", "yellow"),
                        Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej", "color", "green"),
                        Map.of("value", "CANCELLED", "label", "Anulowane", "color", "red")
                ),
                "allowedTransitions", Map.of(
                        "PENDING", List.of("CONFIRMED", "CANCELLED"),
                        "CONFIRMED", List.of("PICKED_UP", "CANCELLED"),
                        "PICKED_UP", List.of("IN_SERVICE", "ON_THE_WAY_BACK", "CANCELLED"),
                        "IN_SERVICE", List.of("ON_THE_WAY_BACK"),
                        "ON_THE_WAY_BACK", List.of(),
                        "CANCELLED", List.of()
                )
        ));
    }

    /**
     * Sprawdza uprawnienia administratora
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> getAdminPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isModerator = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR"));

        return ResponseEntity.ok(Map.of(
                "canViewOrders", true,
                "canModifyOrders", true,
                "canDeleteOrders", isAdmin,
                "canManageSlots", isAdmin,
                "canGenerateReports", true,
                "userRole", isAdmin ? "ADMIN" : "MODERATOR"
        ));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}