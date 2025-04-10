package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.service.ServiceOrderService;
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
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    public ServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createServiceOrder(@Valid @RequestBody ServiceOrderDto serviceOrderDto) {
        String email = getCurrentUserEmail();
        return serviceOrderService.createServiceOrder(serviceOrderDto, email);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceOrder> getUserServiceOrders() {
        String email = getCurrentUserEmail();
        return serviceOrderService.getUserServiceOrders(email);
    }

    @GetMapping("/bicycle/{bicycleId}")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceOrder> getBicycleServiceOrders(@PathVariable Long bicycleId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.getBicycleServiceOrders(bicycleId, email);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ServiceOrder> getServiceOrderById(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.getServiceOrderById(orderId, email);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> cancelServiceOrder(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.cancelServiceOrder(orderId, email);
    }

    @GetMapping("/package-price/{packageType}")
    public ResponseEntity<?> getServicePackagePrice(@PathVariable String packageType) {
        try {
            ServiceOrder.ServicePackage servicePackage = ServiceOrder.ServicePackage.valueOf(packageType.toUpperCase());
            return serviceOrderService.getServicePackagePrice(servicePackage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid service package type"));
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}