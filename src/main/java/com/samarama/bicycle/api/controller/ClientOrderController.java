package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.OrderManagementService;
import com.samarama.bicycle.api.service.BikeServiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CLIENT')")
public class ClientOrderController {

    private final OrderManagementService orderManagementService;
    private final BikeServiceService bikeServiceService;

    public ClientOrderController(OrderManagementService orderManagementService, BikeServiceService bikeServiceService) {
        this.orderManagementService = orderManagementService;
        this.bikeServiceService = bikeServiceService;
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

    // === TWORZENIE NOWYCH ZAMÓWIEŃ ===

    /**
     * Tworzy nowe zamówienie serwisowe
     */
    @PostMapping("/service")
    public ResponseEntity<?> createServiceOrder(@Valid @RequestBody ServiceOrderDto serviceOrderDto) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.createServiceOrder(serviceOrderDto, userEmail);
    }

    /**
     * Tworzy nowe zamówienie transportowe (TYLKO transport)
     */
    @PostMapping("/transport")
    public ResponseEntity<?> createTransportOrder(@Valid @RequestBody TransportOrderDto transportOrderDto) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.createTransportOrder(transportOrderDto, userEmail);
    }

    // === MODYFIKACJA ZAMÓWIEŃ SERWISOWYCH ===

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
     * Pobiera szczegóły zamówienia serwisowego
     */
    @GetMapping("/service/{orderId}")
    public ResponseEntity<ServiceAndTransportOrdersDto> getServiceOrderDetails(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.getOrderDetails(orderId, userEmail, false);
    }

    /**
     * Anuluje zamówienie serwisowe
     */
    @DeleteMapping("/service/{orderId}")
    public ResponseEntity<?> cancelServiceOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.cancelUserOrder(orderId, userEmail);
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

        String userEmail = getCurrentUserEmail();
        return orderManagementService.updateServiceOrderStatus(orderId, newStatus, userEmail);
    }

    // === MODYFIKACJA ZAMÓWIEŃ TRANSPORTOWYCH ===

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
     * Pobiera szczegóły zamówienia transportowego
     */
    @GetMapping("/transport/{orderId}")
    public ResponseEntity<ServiceAndTransportOrdersDto> getTransportOrderDetails(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.getOrderDetails(orderId, userEmail, false);
    }

    /**
     * Anuluje zamówienie transportowe
     */
    @DeleteMapping("/transport/{orderId}")
    public ResponseEntity<?> cancelTransportOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.cancelUserOrder(orderId, userEmail);
    }

    /**
     * Zmienia status zamówienia transportowego
     */
    @PatchMapping("/transport/{orderId}/status")
    public ResponseEntity<?> updateTransportOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        String userEmail = getCurrentUserEmail();
        return orderManagementService.updateTransportOrderStatus(orderId, newStatus, userEmail);
    }

    // === UNIWERSALNE OPERACJE ===

    /**
     * Anuluje zamówienie klienta (uniwersalne - działa dla serwisowych i transportowych)
     * Można anulować tylko w statusie PENDING lub CONFIRMED
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelUserOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return orderManagementService.cancelUserOrder(orderId, userEmail);
    }

    // === ENDPOINTY POMOCNICZE DLA TRANSPORTU ===

    /**
     * Pobiera dostępne serwisy dla transportu (publiczny endpoint)
     */
    @GetMapping("/transport/available-services")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<BikeServicePinDto>> getAvailableServices() {
        List<BikeServicePinDto> services = bikeServiceService.getAllBikeServicePins();
        return ResponseEntity.ok(services);
    }

    /**
     * Pobiera szczegółowe informacje o serwisie
     */
    @GetMapping("/transport/service/{serviceId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<BikeServiceDto> getServiceDetails(@PathVariable Long serviceId) {
        return bikeServiceService.getBikeServiceDetails(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Oblicza koszt transportu (TYLKO transport)
     */
    @PostMapping("/transport/calculate-cost")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> calculateTransportCost(@RequestBody Map<String, Object> request) {
        try {
            // Pobierz parametry z requestu
            Integer bicycleCount = (Integer) request.get("bicycleCount");
            Double distance = request.get("distance") != null ?
                    ((Number) request.get("distance")).doubleValue() : null;

            if (bicycleCount == null || bicycleCount <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Liczba rowerów jest wymagana i musi być większa od 0"
                ));
            }

            // Kalkulacja kosztów TYLKO transportu
            BigDecimal baseCost = new BigDecimal("30.00"); // 30 zł za podstawowy transport
            BigDecimal perBikeCost = new BigDecimal("15.00"); // 15 zł za każdy dodatkowy rower
            BigDecimal perKmCost = new BigDecimal("2.50"); // 2.50 zł za km

            BigDecimal totalCost = baseCost;

            // Koszt za dodatkowe rowery (pierwszy w cenie bazowej)
            if (bicycleCount > 1) {
                totalCost = totalCost.add(perBikeCost.multiply(new BigDecimal(bicycleCount - 1)));
            }

            // Koszt za dystans
            if (distance != null && distance > 0) {
                totalCost = totalCost.add(perKmCost.multiply(new BigDecimal(distance)));
            }

            return ResponseEntity.ok(Map.of(
                    "transportCost", totalCost,
                    "breakdown", Map.of(
                            "baseCost", baseCost,
                            "additionalBikes", bicycleCount > 1 ? perBikeCost.multiply(new BigDecimal(bicycleCount - 1)) : BigDecimal.ZERO,
                            "distanceCost", distance != null ? perKmCost.multiply(new BigDecimal(distance)) : BigDecimal.ZERO
                    ),
                    "currency", "PLN",
                    "description", "Koszt transportu do zewnętrznego serwisu"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Błąd podczas kalkulacji kosztów: " + e.getMessage()
            ));
        }
    }

    /**
     * Tworzy zamówienie transportowe dla gości (TYLKO transport)
     */
    @PostMapping("/transport/guest")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> createGuestTransportOrder(@Valid @RequestBody TransportOrderDto transportOrderDto) {
        return orderManagementService.createGuestTransportOrder(transportOrderDto);
    }

    // === INFORMACJE O OGRANICZENIACH ===

    /**
     * Informacje o ograniczeniach modyfikacji zamówień
     */
    @GetMapping("/modification-rules")
    public ResponseEntity<?> getModificationRules() {
        return ResponseEntity.ok(Map.of(
                "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "IN_TRANSPORT", "DELIVERED_TO_SERVICE", "COMPLETED", "DELIVERED", "CANCELLED"),
                "message", "Zamówienia można modyfikować lub anulować tylko w statusie PENDING lub CONFIRMED",
                "supportedOrderTypes", List.of("SERVICE", "TRANSPORT", "COMBINED"),
                "transportStatuses", List.of("PENDING", "CONFIRMED", "PICKED_UP", "IN_TRANSPORT", "DELIVERED_TO_SERVICE", "COMPLETED", "CANCELLED"),
                "serviceStatuses", List.of("PENDING", "CONFIRMED", "PICKED_UP", "IN_SERVICE", "COMPLETED", "DELIVERED", "CANCELLED")
        ));
    }

    /**
     * Pobiera dostępne statusy dla zamówień transportowych
     */
    @GetMapping("/transport/statuses")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getTransportStatuses() {
        return ResponseEntity.ok(List.of(
                Map.of("value", "PENDING", "label", "Oczekujące"),
                Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                Map.of("value", "PICKED_UP", "label", "Odebrane"),
                Map.of("value", "IN_TRANSPORT", "label", "W transporcie"),
                Map.of("value", "DELIVERED_TO_SERVICE", "label", "Dostarczone do serwisu"),
                Map.of("value", "COMPLETED", "label", "Zakończone"),
                Map.of("value", "CANCELLED", "label", "Anulowane")
        ));
    }

    /**
     * Pobiera dostępne statusy dla zamówień serwisowych
     */
    @GetMapping("/service/statuses")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getServiceStatuses() {
        return ResponseEntity.ok(List.of(
                Map.of("value", "PENDING", "label", "Oczekujące"),
                Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                Map.of("value", "PICKED_UP", "label", "Odebrane"),
                Map.of("value", "IN_SERVICE", "label", "W serwisie"),
                Map.of("value", "COMPLETED", "label", "Zakończone"),
                Map.of("value", "DELIVERED", "label", "Dostarczone"),
                Map.of("value", "CANCELLED", "label", "Anulowane")
        ));
    }

    // === HELPER METHODS ===

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}