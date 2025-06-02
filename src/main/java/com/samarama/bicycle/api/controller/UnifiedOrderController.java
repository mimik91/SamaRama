package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.UnifiedOrderService;
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
@RequestMapping("/api/unified-orders")
public class UnifiedOrderController {

    private final UnifiedOrderService unifiedOrderService;

    public UnifiedOrderController(UnifiedOrderService unifiedOrderService) {
        this.unifiedOrderService = unifiedOrderService;
    }

    // === PUBLICZNE ENDPOINTY ===

    /**
     * Pobiera dostępne serwisy dla transportu
     */
    @GetMapping("/available-services")
    public ResponseEntity<List<BikeServicePinDto>> getAvailableServices() {
        return unifiedOrderService.getAvailableServices();
    }

    /**
     * Pobiera dostępne pakiety serwisowe
     */
    @GetMapping("/service-packages")
    public ResponseEntity<List<ServicePackageDto>> getServicePackages() {
        return unifiedOrderService.getServicePackages();
    }

    /**
     * Pobiera szczegóły serwisu
     */
    @GetMapping("/service-details/{serviceId}")
    public ResponseEntity<BikeServiceDto> getServiceDetails(@PathVariable Long serviceId) {
        return unifiedOrderService.getServiceDetails(serviceId);
    }

    /**
     * Sprawdza dostępność slotów na dzień
     */
    @GetMapping("/availability/{date}")
    public ResponseEntity<?> checkAvailability(
            @PathVariable String date,
            @RequestParam(defaultValue = "1") int bikesCount) {
        return unifiedOrderService.checkAvailability(date, bikesCount);
    }

    /**
     * Oblicza koszt transportu
     */
    @PostMapping("/calculate-transport-cost")
    public ResponseEntity<?> calculateTransportCost(@RequestBody Map<String, Object> request) {
        return unifiedOrderService.calculateTransportCost(request);
    }

    // === ENDPOINTY DLA ZALOGOWANYCH UŻYTKOWNIKÓW ===

    /**
     * Pobiera adresy użytkownika
     */
    @GetMapping("/my/addresses")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<AddressDto>> getMyAddresses() {
        return unifiedOrderService.getUserAddresses(getCurrentUserEmail());
    }

    /**
     * Pobiera wszystkie zamówienia użytkownika
     */
    @GetMapping("/my/orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllMyOrders() {
        return unifiedOrderService.getUserOrders(getCurrentUserEmail());
    }

    /**
     * Pobiera szczegóły zamówienia
     */
    @GetMapping("/my/orders/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<UnifiedOrderResponseDto> getOrderDetails(@PathVariable Long orderId) {
        return unifiedOrderService.getUserOrderDetails(orderId, getCurrentUserEmail());
    }

    /**
     * Uniwersalne tworzenie zamówienia (transport lub serwis)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody ServiceOrTransportOrderDto dto) {
        return unifiedOrderService.createUserOrder(dto, getCurrentUserEmail());
    }

    /**
     * Uniwersalne anulowanie zamówienia
     */
    @DeleteMapping("/my/orders/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        return unifiedOrderService.cancelUserOrder(orderId, getCurrentUserEmail());
    }

    /**
     * Statystyki użytkownika
     */
    @GetMapping("/my/statistics")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getMyOrderStatistics() {
        return unifiedOrderService.getUserStatistics(getCurrentUserEmail());
    }

    // === ENDPOINTY DLA GOŚCI ===

    /**
     * Tworzy zamówienie dla gościa (zawsze serwisowe)
     */
    @PostMapping("/guest/create")
    public ResponseEntity<?> createGuestOrder(@Valid @RequestBody ServiceOrTransportOrderDto dto) {
        return unifiedOrderService.createGuestOrder(dto);
    }

    // === ENDPOINTY ADMINISTRACYJNE ===

    /**
     * Pobiera wszystkie zamówienia (admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllOrdersForAdmin() {
        return unifiedOrderService.getAllOrdersForAdmin();
    }

    /**
     * Wyszukuje zamówienia po email/telefonie klienta (admin)
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UnifiedOrderResponseDto>> searchOrders(@RequestParam String searchTerm) {
        return unifiedOrderService.searchOrders(searchTerm);
    }

    /**
     * Zmienia status zamówienia (admin)
     */
    @PatchMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        return unifiedOrderService.updateOrderStatus(orderId, request.get("status"), getCurrentUserEmail());
    }

    // === OPERACJE SERWISOWE (dla serwisantów i adminów) ===

    /**
     * Rozpoczyna serwis
     */
    @PostMapping("/admin/orders/{orderId}/start-service")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> startService(@PathVariable Long orderId) {
        return unifiedOrderService.startService(orderId, getCurrentUserEmail());
    }

    /**
     * Kończy serwis
     */
    @PostMapping("/admin/orders/{orderId}/complete-service")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> completeService(@PathVariable Long orderId) {
        return unifiedOrderService.completeService(orderId, getCurrentUserEmail());
    }

    /**
     * Aktualizuje notatki serwisowe
     */
    @PatchMapping("/admin/orders/{orderId}/service-notes")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<?> updateServiceNotes(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        return unifiedOrderService.updateServiceNotes(orderId, request.get("notes"), getCurrentUserEmail());
    }

    // === ENDPOINTY POMOCNICZE ===

    /**
     * Pobiera dostępne statusy i zasady
     */
    @GetMapping("/info")
    public ResponseEntity<?> getOrderInfo() {
        return unifiedOrderService.getOrderInfo();
    }

    // === HELPER METHOD ===

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}isowe
        return serviceOrderService.createGuestServiceOrder(dto);
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
@PatchMapping("/admin/orders/{orderId}/status")
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
@PostMapping("/admin/orders/{orderId}/start-service")
@PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
public ResponseEntity<?> startService(@PathVariable Long orderId) {
    String userEmail = getCurrentUserEmail();
    return serviceOrderService.startService(orderId, userEmail);
}

/**
 * Kończy serwis
 */
@PostMapping("/admin/orders/{orderId}/complete-service")
@PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
public ResponseEntity<?> completeService(@PathVariable Long orderId) {
    String userEmail = getCurrentUserEmail();
    return serviceOrderService.completeService(orderId, userEmail);
}

/**
 * Aktualizuje notatki serwisowe
 */
@PatchMapping("/admin/orders/{orderId}/service-notes")
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
            "orderTypes", Map.of(
                    "TRANSPORT", "Transport do zewnętrznego serwisu",
                    "SERVICE", "Transport + serwis w naszym serwisie"
            ),
            "statuses", List.of(
                    Map.of("value", "PENDING", "label", "Oczekujące"),
                    Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                    Map.of("value", "PICKED_UP", "label", "Odebrane"),
                    Map.of("value", "IN_SERVICE", "label", "W serwisie"),
                    Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej"),
                    Map.of("value", "CANCELLED", "label", "Anulowane")
            ),
            "pickupTime", "18:00 - 22:00"
    ));
}

/**
 * Pobiera zasady modyfikacji zamówień
 */
@GetMapping("/rules")
public ResponseEntity<?> getOrderRules() {
    return ResponseEntity.ok(Map.of(
            "pickupTime", Map.of(
                    "from", "18:00",
                    "to", "22:00",
                    "description", "Stały czas odbioru między 18:00 a 22:00"
            ),
            "modification", Map.of(
                    "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                    "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "ON_THE_WAY_BACK", "CANCELLED"),
                    "message", "Zamówienia można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ),
            "serviceTypes", Map.of(
                    "transport", Map.of(
                            "description", "Tylko transport do zewnętrznego serwisu",
                            "targetService", "Nie może być serwis własny (ID=1)"
                    ),
                    "service", Map.of(
                            "description", "Transport + serwis w naszym serwisie",
                            "targetService", "Zawsze serwis własny (ID=1)",
                            "requiresPackage", true
                    )
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
    long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equals(o.status())).count();

    return ResponseEntity.ok(Map.of(
            "totalOrders", totalOrders,
            "transportOrders", transportOrders,
            "serviceOrders", serviceOrders,
            "pendingOrders", pendingOrders,
            "completedOrders", completedOrders,
            "cancelledOrders", cancelledOrders,
            "activeOrders", totalOrders - completedOrders - cancelledOrders
    ));
}

// === HELPER METHODS ===

private String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
}

private Long getCurrentUserId() {
    String email = getCurrentUserEmail();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    return user.getId();
}

/**
 * Konwertuje ServiceOrTransportOrderDto do TransportOrderDto dla backward compatibility
 */
private ResponseEntity<?> createTransportOrderFromDto(ServiceOrTransportOrderDto dto, String userEmail) {
    // Tworzy TransportOrderDto z danych ServiceOrTransportOrderDto
    TransportOrderDto transportDto = new TransportOrderDto(
            dto.bicycleIds(),
            dto.bicycles(),
            dto.pickupDate(),
            dto.getPickupAddressString(),
            dto.pickupLatitude(),
            dto.pickupLongitude(),
            null, // pickupTimeFrom - stałe 18:00
            null, // pickupTimeTo - stałe 22:00
            null, // deliveryAddress - będzie ustawiony automatycznie
            null, // deliveryLatitude
            null, // deliveryLongitude
            dto.targetServiceId(),
            dto.transportPrice(),
            60, // estimatedTime
            dto.transportNotes(),
            dto.additionalNotes(),
            dto.clientEmail(),
            dto.clientPhone(),
            null, // clientName - nie używamy
            dto.pickupCity()
    );

    return transportOrderService.createTransportOrder(transportDto, userEmail);
}
}isowe
        return serviceOrderService.createGuestServiceOrder(dto);
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
@PatchMapping("/admin/orders/{orderId}/status")
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
@PostMapping("/admin/orders/{orderId}/start-service")
@PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
public ResponseEntity<?> startService(@PathVariable Long orderId) {
    String userEmail = getCurrentUserEmail();
    return serviceOrderService.startService(orderId, userEmail);
}

/**
 * Kończy serwis
 */
@PostMapping("/admin/orders/{orderId}/complete-service")
@PreAuthorize("hasAnyRole('SERVICE', 'ADMIN', 'MODERATOR')")
public ResponseEntity<?> completeService(@PathVariable Long orderId) {
    String userEmail = getCurrentUserEmail();
    return serviceOrderService.completeService(orderId, userEmail);
}

/**
 * Aktualizuje notatki serwisowe
 */
@PatchMapping("/admin/orders/{orderId}/service-notes")
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
            "orderTypes", Map.of(
                    "TRANSPORT", "Transport do zewnętrznego serwisu",
                    "SERVICE", "Transport + serwis w naszym serwisie"
            ),
            "statuses", List.of(
                    Map.of("value", "PENDING", "label", "Oczekujące"),
                    Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                    Map.of("value", "PICKED_UP", "label", "Odebrane"),
                    Map.of("value", "IN_SERVICE", "label", "W serwisie"),
                    Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej"),
                    Map.of("value", "CANCELLED", "label", "Anulowane")
            ),
            "pickupTime", "18:00 - 22:00"
    ));
}

/**
 * Pobiera zasady modyfikacji zamówień
 */
@GetMapping("/rules")
public ResponseEntity<?> getOrderRules() {
    return ResponseEntity.ok(Map.of(
            "pickupTime", Map.of(
                    "from", "18:00",
                    "to", "22:00",
                    "description", "Stały czas odbioru między 18:00 a 22:00"
            ),
            "modification", Map.of(
                    "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                    "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "ON_THE_WAY_BACK", "CANCELLED"),
                    "message", "Zamówienia można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ),
            "serviceTypes", Map.of(
                    "transport", Map.of(
                            "description", "Tylko transport do zewnętrznego serwisu",
                            "targetService", "Nie może być serwis własny (ID=1)"
                    ),
                    "service", Map.of(
                            "description", "Transport + serwis w naszym serwisie",
                            "targetService", "Zawsze serwis własny (ID=1)",
                            "requiresPackage", true
                    )
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
    long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equals(o.status())).count();

    return ResponseEntity.ok(Map.of(
            "totalOrders", totalOrders,
            "transportOrders", transportOrders,
            "serviceOrders", serviceOrders,
            "pendingOrders", pendingOrders,
            "completedOrders", completedOrders,
            "cancelledOrders", cancelledOrders,
            "activeOrders", totalOrders - completedOrders - cancelledOrders
    ));
}

// === HELPER METHODS ===

private String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
}

private Long getCurrentUserId() {
    String email = getCurrentUserEmail();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    return user.getId();
}

/**
 * Konwertuje ServiceOrTransportOrderDto do TransportOrderDto dla backward compatibility
 */
private ResponseEntity<?> createTransportOrderFromDto(ServiceOrTransportOrderDto dto, String userEmail) {
    // Tworzy TransportOrderDto z danych ServiceOrTransportOrderDto
    TransportOrderDto transportDto = new TransportOrderDto(
            dto.bicycleIds(),
            dto.bicycles(),
            dto.pickupDate(),
            dto.getPickupAddressString(),
            dto.pickupLatitude(),
            dto.pickupLongitude(),
            null, // pickupTimeFrom - stałe 18:00
            null, // pickupTimeTo - stałe 22:00
            null, // deliveryAddress - będzie ustawiony automatycznie
            null, // deliveryLatitude
            null, // deliveryLongitude
            dto.targetServiceId(),
            dto.transportPrice(),
            60, // estimatedTime
            dto.transportNotes(),
            dto.additionalNotes(),
            dto.clientEmail(),
            dto.clientPhone(),
            null, // clientName - nie używamy
            dto.pickupCity()
    );

    return transportOrderService.createTransportOrder(transportDto, userEmail);
}
