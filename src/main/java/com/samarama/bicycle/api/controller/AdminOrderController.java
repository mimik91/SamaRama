package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminOrderController {

    private final ServiceOrderService serviceOrderService;

    public AdminOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceOrderResponseDto>> getAllOrders() {
        return ResponseEntity.ok(serviceOrderService.getAllServiceOrders());
    }

    /**
     * Pobierz szczegóły zamówienia po ID (dla administratora)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ServiceOrderResponseDto> getOrderById(@PathVariable Long orderId) {
        return serviceOrderService.getServiceOrderByIdForAdmin(orderId);
    }

    /**
     * Aktualizuj szczegóły zamówienia (dla administratora)
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ServiceOrderDto serviceOrderDto) {

        String email = getCurrentUserEmail();
        return serviceOrderService.updateServiceOrderByAdmin(orderId, serviceOrderDto, email);
    }

    /**
     * Anuluj zamówienie (dla administratora)
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.cancelServiceOrderByAdmin(orderId, email);
    }

    /**
     * Aktualizuj status zamówienia (dla administratora)
     */
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
            return serviceOrderService.updateOrderStatusByAdmin(orderId, newStatus, email);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy status: " + statusStr));
        }
    }

    /**
     * Pobierz email zalogowanego użytkownika
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}