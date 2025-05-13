package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.service.ServiceOrderService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;
    private final BicycleRepository bicycleRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;

    public ServiceOrderController(
            @Lazy ServiceOrderService serviceOrderService,
            BicycleRepository bicycleRepository,
            IncompleteBikeRepository incompleteBikeRepository
    ) {
        this.serviceOrderService = serviceOrderService;
        this.bicycleRepository = bicycleRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
    }

    // --- REGULAR USER ENDPOINTS ---

    @PostMapping
    public ResponseEntity<?> createServiceOrder(@Valid @RequestBody ServiceOrderDto serviceOrderDto) {
        String email = getCurrentUserEmail();
        return serviceOrderService.createServiceOrder(serviceOrderDto, email);
    }

    @GetMapping("/bicycle/{bicycleId}")
    public ResponseEntity<List<ServiceOrderResponseDto>> getBicycleServiceOrders(@PathVariable Long bicycleId) {
        String currentUserEmail = getCurrentUserEmail();

        // Najpierw sprawdź, czy istnieje kompletny rower o podanym ID
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(bicycleId);

        if (bicycleOpt.isPresent()) {
            // Jeśli znaleziono kompletny rower, pobierz jego historię zamówień
            List<ServiceOrderResponseDto> orders = serviceOrderService.getBicycleServiceOrders(bicycleId, currentUserEmail);
            return ResponseEntity.ok(orders);
        } else {
            // Jeśli nie znaleziono kompletnego roweru, sprawdź czy istnieje niekompletny rower
            Optional<IncompleteBike> incompleteBikeOpt = incompleteBikeRepository.findById(bicycleId);

            if (incompleteBikeOpt.isPresent()) {
                // Dla niekompletnych rowerów, zwracamy pustą listę zamówień
                return ResponseEntity.ok(List.of());
            }

            // Jeśli nie znaleziono żadnego roweru, zwróć 404
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ServiceOrderResponseDto>> getUserServiceOrders() {
        String email = getCurrentUserEmail();
        List<ServiceOrderResponseDto> orders = serviceOrderService.getUserServiceOrders(email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ServiceOrderResponseDto> getServiceOrderById(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();

        // Sprawdzamy, czy użytkownik ma rolę ADMIN lub MODERATOR
        if (hasAdminOrModeratorRole()) {
            // Dla admina używamy specjalnej metody bez sprawdzania właściciela
            return serviceOrderService.getServiceOrderByIdForAdmin(orderId);
        } else {
            // Dla zwykłego użytkownika sprawdzamy, czy jest właścicielem
            return serviceOrderService.getServiceOrderById(orderId, email);
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelServiceOrder(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();

        // Sprawdzamy, czy użytkownik ma rolę ADMIN lub MODERATOR
        if (hasAdminOrModeratorRole()) {
            // Dla admina używamy specjalnej metody
            return serviceOrderService.cancelServiceOrderByAdmin(orderId, email);
        } else {
            // Dla zwykłego użytkownika
            return serviceOrderService.cancelServiceOrder(orderId, email);
        }
    }

    @GetMapping("/package-price/{packageCode}")
    public ResponseEntity<?> getServicePackagePrice(@PathVariable String packageCode) {
        return serviceOrderService.getServicePackagePrice(packageCode);
    }

    // --- ADMIN ENDPOINTS ---

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<ServiceOrderResponseDto>> getAllOrders() {
        return ResponseEntity.ok(serviceOrderService.getAllServiceOrders());
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateServiceOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ServiceOrderDto serviceOrderDto) {

        String email = getCurrentUserEmail();

        // Sprawdzamy, czy użytkownik ma rolę ADMIN lub MODERATOR
        if (hasAdminOrModeratorRole()) {
            // Dla admina używamy specjalnej metody
            return serviceOrderService.updateServiceOrderByAdmin(orderId, serviceOrderDto, email);
        } else {
            // Dla zwykłego użytkownika
            return serviceOrderService.updateServiceOrder(orderId, serviceOrderDto, email);
        }
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String statusStr = request.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        try {
            ServiceOrder.OrderStatus newStatus = ServiceOrder.OrderStatus.valueOf(statusStr);
            String email = getCurrentUserEmail();

            // Sprawdzamy, czy użytkownik ma rolę ADMIN lub MODERATOR
            if (hasAdminOrModeratorRole()) {
                // Dla admina używamy specjalnej metody
                return serviceOrderService.updateOrderStatusByAdmin(orderId, newStatus, email);
            } else {
                // Dla zwykłego użytkownika
                return serviceOrderService.updateOrderStatus(orderId, newStatus, email);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy status: " + statusStr));
        }
    }

    // --- HELPER METHODS ---

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean hasAdminOrModeratorRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MODERATOR"));
    }
}