package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.service.ServiceOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;
    private final BicycleRepository bicycleRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;

    public ServiceOrderController(
            ServiceOrderService serviceOrderService,
            BicycleRepository bicycleRepository,
            IncompleteBikeRepository incompleteBikeRepository
    ) {
        this.serviceOrderService = serviceOrderService;
        this.bicycleRepository = bicycleRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
    }

    @PostMapping
    public ResponseEntity<?> createServiceOrder(@Valid @RequestBody ServiceOrderDto serviceOrderDto) {
        String email = getCurrentUserEmail();
        return serviceOrderService.createServiceOrder(serviceOrderDto, email);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Authentication is null - user not authenticated");
        }
        return authentication.getName();
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
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ServiceOrderResponseDto> getServiceOrderById(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.getServiceOrderById(orderId, email);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> cancelServiceOrder(@PathVariable Long orderId) {
        String email = getCurrentUserEmail();
        return serviceOrderService.cancelServiceOrder(orderId, email);
    }

    @GetMapping("/package-price/{packageCode}")
    public ResponseEntity<?> getServicePackagePrice(@PathVariable String packageCode) {
        return serviceOrderService.getServicePackagePrice(packageCode);
    }
}