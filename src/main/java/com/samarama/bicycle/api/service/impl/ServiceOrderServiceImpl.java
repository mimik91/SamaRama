package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.ServiceOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BikeServiceRepository bikeServiceRepository;

    // Ceny pakietów usług
    private static final Map<ServiceOrder.ServicePackage, BigDecimal> PACKAGE_PRICES = new HashMap<>();

    static {
        PACKAGE_PRICES.put(ServiceOrder.ServicePackage.BASIC, new BigDecimal("200.00"));
        PACKAGE_PRICES.put(ServiceOrder.ServicePackage.EXTENDED, new BigDecimal("350.00"));
        PACKAGE_PRICES.put(ServiceOrder.ServicePackage.FULL, new BigDecimal("600.00"));
    }

    public ServiceOrderServiceImpl(ServiceOrderRepository serviceOrderRepository,
                                   UserRepository userRepository,
                                   IncompleteBikeRepository incompleteBikeRepository,
                                   BikeServiceRepository bikeServiceRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrderDto serviceOrderDto, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

            // Szukamy roweru w tabeli incomplete_bikes
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(serviceOrderDto.bicycleId());
            if (bikeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Bicycle not found"));
            }

            IncompleteBike bike = bikeOpt.get();

            // Sprawdź czy rower należy do użytkownika
            if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You do not have permission to order service for this bicycle"));
            }

            // Sprawdź czy data odbioru jest w przyszłości
            if (serviceOrderDto.pickupDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Pickup date must be in the future"));
            }

            // Próba znalezienia najbliższego dostępnego serwisu (uproszenie - bierzemy pierwszy z listy)
            List<BikeService> services = bikeServiceRepository.findAll();
            if (services.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "No bike services available"));
            }

            // W rzeczywistym systemie warto byłoby zaimplementować bardziej zaawansowaną logikę wyboru serwisu
            BikeService bikeService = services.get(0);

            // Utwórz nowe zamówienie serwisu
            ServiceOrder serviceOrder = new ServiceOrder();
            serviceOrder.setBicycle(bike);
            serviceOrder.setClient(user);
            serviceOrder.setService(bikeService);
            serviceOrder.setServicePackage(serviceOrderDto.servicePackage());
            serviceOrder.setPickupDate(serviceOrderDto.pickupDate());
            serviceOrder.setPickupAddress(serviceOrderDto.pickupAddress());
            serviceOrder.setPickupLatitude(serviceOrderDto.pickupLatitude());
            serviceOrder.setPickupLongitude(serviceOrderDto.pickupLongitude());
            serviceOrder.setAdditionalNotes(serviceOrderDto.additionalNotes());
            serviceOrder.setPrice(PACKAGE_PRICES.get(serviceOrderDto.servicePackage()));

            // Zapisz zamówienie
            ServiceOrder savedOrder = serviceOrderRepository.save(serviceOrder);

            return ResponseEntity.ok(Map.of(
                    "message", "Service order created successfully",
                    "orderId", savedOrder.getId()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating service order: " + e.getMessage()));
        }
    }

    @Override
    public List<ServiceOrder> getUserServiceOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        return serviceOrderRepository.findByClient(user);
    }

    @Override
    public List<ServiceOrder> getBicycleServiceOrders(Long bicycleId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

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
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelServiceOrder(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You do not have permission to cancel this order"));
        }

        // Sprawdź czy zamówienie można anulować (tylko w stanie PENDING lub CONFIRMED)
        if (order.getStatus() != ServiceOrder.OrderStatus.PENDING &&
                order.getStatus() != ServiceOrder.OrderStatus.CONFIRMED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Service order cannot be cancelled in current state: " + order.getStatus()));
        }

        // Anuluj zamówienie
        order.setStatus(ServiceOrder.OrderStatus.CANCELLED);
        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Service order cancelled successfully"));
    }

    @Override
    public ResponseEntity<?> getServicePackagePrice(ServiceOrder.ServicePackage servicePackage) {
        BigDecimal price = PACKAGE_PRICES.get(servicePackage);
        if (price == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid service package"));
        }

        return ResponseEntity.ok(Map.of(
                "servicePackage", servicePackage,
                "price", price
        ));
    }
}