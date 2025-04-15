package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
    private static final Logger logger = Logger.getLogger(ServiceOrderServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final UserRepository userRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServicePackageRepository servicePackageRepository;

    public ServiceOrderServiceImpl(ServiceOrderRepository serviceOrderRepository,
                                   IncompleteBikeRepository incompleteBikeRepository,
                                   UserRepository userRepository,
                                   BikeServiceRepository bikeServiceRepository,
                                   ServicePackageRepository servicePackageRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.userRepository = userRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    @Override
    public List<ServiceOrder> getUserServiceOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        return serviceOrderRepository.findByClient(user);
    }

    @Override
    public List<ServiceOrder> getBicycleServiceOrders(Long bicycleId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
        if (bikeOpt.isEmpty()) {
            return List.of();
        }

        IncompleteBike bike = bikeOpt.get();

        // Sprawdź czy rower należy do użytkownika
        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return List.of();
        }

        return serviceOrderRepository.findByBicycle(bike);
    }

    @Override
    public ResponseEntity<ServiceOrder> getServiceOrderById(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(order);
    }

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrderDto serviceOrderDto, String userEmail) {
        // Validate service date is not in the past or too far in the future
        LocalDate today = LocalDate.now();
        if (serviceOrderDto.pickupDate().isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Pickup date cannot be in the past"));
        }

        LocalDate maxDate = today.plusMonths(1);
        if (serviceOrderDto.pickupDate().isAfter(maxDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Pickup date cannot be more than a month in the future"));
        }

        // Get the bicycle (incomplete bike)
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(serviceOrderDto.bicycleId());
        if (bikeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bicycle not found"));
        }

        IncompleteBike bike = bikeOpt.get();

        // Get the current user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        // Verify ownership of the bicycle
        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to order service for this bicycle"));
        }

        // Get the service package
        ServicePackage servicePackage = null;

        // Try to find by ID first
        if (serviceOrderDto.servicePackageId() != null) {
            servicePackage = servicePackageRepository.findById(serviceOrderDto.servicePackageId())
                    .orElse(null);
        }

        // If not found by ID, try by code
        if (servicePackage == null && serviceOrderDto.servicePackageCode() != null) {
            servicePackage = servicePackageRepository.findByCode(serviceOrderDto.servicePackageCode())
                    .orElse(null);
        }

        // If still not found, return error
        if (servicePackage == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid service package"));
        }

        // For simplicity we'll just use the first available service
        // In a real app, we'd have a proper service selection mechanism
        List<BikeService> services = bikeServiceRepository.findAll();
        if (services.isEmpty()) {
            return ResponseEntity.status(500).body(Map.of("message", "No service providers available"));
        }
        BikeService bikeService = services.get(0);

        // Create the service order
        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setBicycle(bike);
        serviceOrder.setClient(user);
        serviceOrder.setService(bikeService);
        serviceOrder.setServicePackage(servicePackage);
        serviceOrder.setServicePackageCode(servicePackage.getCode()); // Store the code for backward compatibility
        serviceOrder.setPickupDate(serviceOrderDto.pickupDate());
        serviceOrder.setPickupAddress(serviceOrderDto.pickupAddress());
        serviceOrder.setPickupLatitude(serviceOrderDto.pickupLatitude());
        serviceOrder.setPickupLongitude(serviceOrderDto.pickupLongitude());
        serviceOrder.setPrice(servicePackage.getPrice());
        serviceOrder.setAdditionalNotes(serviceOrderDto.additionalNotes());
        serviceOrder.setStatus(ServiceOrder.OrderStatus.PENDING);

        ServiceOrder savedOrder = serviceOrderRepository.save(serviceOrder);

        return ResponseEntity.ok(Map.of(
                "message", "Service order created successfully",
                "orderId", savedOrder.getId()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelServiceOrder(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to cancel this order"));
        }

        // Sprawdź czy zamówienie można anulować (tylko w stanie PENDING lub CONFIRMED)
        if (order.getStatus() != ServiceOrder.OrderStatus.PENDING &&
                order.getStatus() != ServiceOrder.OrderStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Service order cannot be cancelled in current state: " + order.getStatus()));
        }

        // Anuluj zamówienie
        order.setStatus(ServiceOrder.OrderStatus.CANCELLED);
        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Service order cancelled successfully"));
    }

    @Override
    public ResponseEntity<?> getServicePackagePrice(ServiceOrder.ServicePackage servicePackage) {
        // To make this backward compatible with the enum version
        String packageCode = servicePackage.toString();

        Optional<ServicePackage> packageEntity = servicePackageRepository.findByCode(packageCode);
        if (packageEntity.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid service package"));
        }

        return ResponseEntity.ok(Map.of(
                "servicePackage", packageCode,
                "price", packageEntity.get().getPrice()
        ));
    }

    // Metoda pomocnicza do konwersji między starym enum a kodem pakietu
    private String convertEnumToCode(ServiceOrder.ServicePackage packageEnum) {
        return packageEnum.name();
    }
}