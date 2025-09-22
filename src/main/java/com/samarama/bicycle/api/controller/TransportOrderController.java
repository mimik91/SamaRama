package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Podstawowy kontroler dla zamówień - obsługuje transport i serwis
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class TransportOrderController {

    private final TransportOrderService transportOrderService;
    private final BikeServiceService bikeServiceService;

    public TransportOrderController(
            TransportOrderService transportOrderService,
            BikeServiceService bikeServiceService) {
        this.transportOrderService = transportOrderService;
        this.bikeServiceService = bikeServiceService;
    }

    // =================== PUBLICZNE ===================

    /**
     * Lista serwisów na mapę
     */
    @GetMapping("/services")
    public ResponseEntity<List<BikeServicePinDto>> getServices() {
        return ResponseEntity.ok(bikeServiceService.getAllBikeServicePins());
    }

    // =================== DLA UŻYTKOWNIKÓW ===================

    /**
     * Moje zamówienia
     */
    @GetMapping("/service")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ServiceOrderResponseDto>> getMyOrders() {
        String userEmail = getCurrentUserEmail();
        List<ServiceOrderResponseDto> orders = transportOrderService.getAllUserOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Nowe zamówienie transportowe (tylko transport)
     */
    @PostMapping("/transport")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createTransportOrder(@Valid @RequestBody ServiceOrTransportOrderDto dto) {
        String userEmail = getCurrentUserEmail();
        return transportOrderService.createTransportOrder(dto, userEmail);
    }

    /**
     * Anuluj zamówienie
     */
    @DeleteMapping("/service/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return transportOrderService.cancelOrder(orderId, userEmail);
    }

    // =================== POMOCNICZE ===================

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @GetMapping("/service/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ServiceOrderDetailsResponseDto> getOrderDetailsUnified(@PathVariable Long orderId) {
        String userEmail = getCurrentUserEmail();
        return transportOrderService.getOrderDetails(orderId, userEmail);
    }

    @GetMapping("courier/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourierOrderDto>> getCourierOrders() {
        String adminEmail = getCurrentUserEmail();
        try {
            List<CourierOrderDto> orders = transportOrderService.getCourierOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }




}