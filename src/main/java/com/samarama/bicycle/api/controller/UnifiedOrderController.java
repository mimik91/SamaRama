package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.BikeServiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class UnifiedOrderController {

    private final TransportOrderService transportOrderService;
    private final ServiceOrderService serviceOrderService;
    private final BikeServiceService bikeServiceService;

    public UnifiedOrderController(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            BikeServiceService bikeServiceService) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.bikeServiceService = bikeServiceService;
    }

    // === PUBLICZNE ENDPOINTY ===

    /**
     * Pobiera dostępne serwisy dla transportu
     */
    @GetMapping("/available-services")
    public ResponseEntity<List<BikeServicePinDto>> getAvailableServices() {
        List<BikeServicePinDto> services = bikeServiceService.getAllBikeServicePins();
        return ResponseEntity.ok(services);
    }

    /**
     * Pobiera szczegóły serwisu
     */
    @GetMapping("/service-details/{serviceId}")
    public ResponseEntity<BikeServiceDto> getServiceDetails(@PathVariable Long serviceId) {
        return bikeServiceService.getBikeServiceDetails(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Sprawdza dostępność slotów na dzień
     */
    @GetMapping("/availability/{date}")
    public ResponseEntity<?> checkAvailability(
            @PathVariable String date,
            @RequestParam(defaultValue = "1") int ordersCount) {
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            boolean available = transportOrderService.areSlotsAvailable(localDate, ordersCount);
            int usedSlots = transportOrderService.countOrdersForDate(localDate);

            return ResponseEntity.ok(Map.of(
                    "date", date,
                    "available", available,
                    "usedSlots", usedSlots,
                    "requestedOrders", ordersCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowa data: " + date));
        }
    }

    // === ENDPOINTY DLA ZALOGOWANYCH UŻYTKOWNIKÓW ===

    /**
     * Pobiera wszystkie zamówienia użytkownika (transport + serwis)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllMyOrders() {
        String userEmail = getCurrentUserEmail();
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllUserOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia transportowe użytkownika
     */
    @GetMapping("/my/transport")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getMyTransportOrders() {
        String userEmail = getCurrentUserEmail();
        List<UnifiedOrderResponseDto> orders = transportOrderService.getUserTransportOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia serwisowe użytkownika
     */
    @GetMapping("/my/service")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getMyServiceOrders() {
        String userEmail = getCurrentUserEmail();
        List<UnifiedOrderResponseDto> orders = serviceOrderService.getUserServiceOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera szczegóły zamówienia
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<UnifiedOrderResponseDto> getOrderDetails(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();

        // Próbuj najpierw jako transport order
        ResponseEntity<UnifiedOrderResponseDto> transportResponse =
                transportOrderService.getOrderDetails(orderId, userEmail);

        if (transportResponse.getStatusCode().is2xxSuccessful()) {
            return transportResponse;
        }

        // Jeśli nie znaleziono, spróbuj jako service order
        return serviceOrderService.getServiceOrderDetails(orderId, userEmail);
    }

    /**
     * Tworzy zamówienie transportowe (TYLKO transport)
     */
    @PostMapping("/transport")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createTransportOrder(@Valid @RequestBody TransportOrderDto dto) {
        String userEmail = getCurrentUserEmail();
        return transportOrderService.createTransportOrder(dto, userEmail);
    }

    /**
     * Tworzy zamówienie serwisowe (transport + serwis)
     */
    @PostMapping("/service")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createServiceOrder(@Valid @RequestBody ServiceOrderDto dto) {
        String userEmail = getCurrentUserEmail();
        return serviceOrderService.createServiceOrder(dto, userEmail);
    }

    /**
     * Uniwersalne anulowanie zamówienia
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();

        // Spróbuj anulować przez transport service (obsługuje oba typy)
        return transportOrderService.cancelOrder(orderId, userEmail);
    }

    // === ENDPOINTY ADMINISTRACYJNE ===

    /**
     * Pobiera wszystkie zamówienia (admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllOrdersForAdmin() {
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia transportowe (admin)
     */
    @GetMapping("/admin/transport")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllTransportOrdersForAdmin() {
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllTransportOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Pobiera tylko zamówienia serwisowe (admin)
     */
    @GetMapping("/admin/service")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllServiceOrdersForAdmin() {
        List<UnifiedOrderResponseDto> orders = serviceOrderService.getAllServiceOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Wyszukuje zamówienia po email/telefonie klienta (admin)
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> searchOrders(@RequestParam String searchTerm) {
        List<UnifiedOrderResponseDto> orders = transportOrderService.searchOrders(searchTerm);
        return ResponseEntity.ok(orders);
    }

    /**
     * Zmienia status zamówienia (admin)
     */
    @PatchMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateOrderStatusByAdmin(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        String adminEmail = getCurrentUserEmail();
        return transportOrderService.updateOrderStatusByAdmin(orderId, newStatus, adminEmail);
    }

    // === OPERACJE SERWISOWE (dla serwisantów i adminów) ===

    /**
     * Rozpoczyna serwis
     */
    @PostMapping("/{orderId}/start-service")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> startService(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return serviceOrderService.startService(orderId, userEmail);
    }

    /**
     * Kończy serwis
     */
    @PostMapping("/{orderId}/complete-service")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> completeService(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return serviceOrderService.completeService(orderId, userEmail);
    }

    /**
     * Aktualizuje notatki serwisowe
     */
    @PatchMapping("/{orderId}/service-notes")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateServiceNotes(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String notes = request.get("notes");
        String userEmail = getCurrentUserEmail();
        return serviceOrderService.updateServiceNotes(orderId, notes, userEmail);
    }

    // === ENDPOINTY POMOCNICZE ===

    /**
     * Pobiera dostępne statusy
     */
    @GetMapping("/statuses")
    public ResponseEntity<?> getOrderStatuses() {
        return ResponseEntity.ok(Map.of(
                "transportStatuses", List.of(
                        Map.of("value", "PENDING", "label", "Oczekujące"),
                        Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                        Map.of("value", "PICKED_UP", "label", "Odebrane"),
                        Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej"),
                        Map.of("value", "CANCELLED", "label", "Anulowane")
                ),
                "serviceStatuses", List.of(
                        Map.of("value", "PENDING", "label", "Oczekujące"),
                        Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                        Map.of("value", "PICKED_UP", "label", "Odebrane"),
                        Map.of("value", "IN_SERVICE", "label", "W serwisie"),
                        Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej"),
                        Map.of("value", "CANCELLED", "label", "Anulowane")
                )
        ));
    }

    /**
     * Pobiera informacje o zasadach modyfikacji zamówień
     */
    @GetMapping("/modification-rules")
    public ResponseEntity<?> getModificationRules() {
        return ResponseEntity.ok(Map.of(
                "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "ON_THE_WAY_BACK", "CANCELLED"),
                "message", "Zamówienia można modyfikować lub anulować tylko w statusie PENDING lub CONFIRMED",
                "orderTypes", Map.of(
                        "TRANSPORT", "Tylko transport do zewnętrznego serwisu",
                        "SERVICE", "Transport + serwis w naszym serwisie"
                )
        ));
    }

    /**
     * Statystyki użytkownika
     */
    @GetMapping("/my/statistics")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getMyOrderStatistics() {
        String userEmail = getCurrentUserEmail();
        List<UnifiedOrderResponseDto> orders = transportOrderService.getAllUserOrders(userEmail);

        long totalOrders = orders.size();
        long transportOrders = orders.stream().filter(o -> "TRANSPORT".equals(o.orderType())).count();
        long serviceOrders = orders.stream().filter(o -> "SERVICE".equals(o.orderType())).count();
        long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.status())).count();
        long completedOrders = orders.stream().filter(o -> "ON_THE_WAY_BACK".equals(o.status())).count();

        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "transportOrders", transportOrders,
                "serviceOrders", serviceOrders,
                "pendingOrders", pendingOrders,
                "completedOrders", completedOrders,
                "activeOrders", totalOrders - completedOrders - orders.stream().filter(o -> "CANCELLED".equals(o.status())).count()
        ));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MODERATOR"));
    }
}