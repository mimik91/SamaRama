package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
    private static final Logger logger = Logger.getLogger(ServiceOrderServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final IncompleteUserRepository incompleteUserRepository;

    public ServiceOrderServiceImpl(ServiceOrderRepository serviceOrderRepository,
                                   IncompleteBikeRepository incompleteBikeRepository,
                                   BicycleRepository bicycleRepository,
                                   UserRepository userRepository,
                                   ServicePackageRepository servicePackageRepository, IncompleteUserRepository incompleteUserRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.incompleteUserRepository = incompleteUserRepository;
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

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        IncompleteUser user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = incompleteUserRepository.findByEmail(userEmail)
                    .orElseGet(() -> {
                        // Jeśli nie istnieje, tworzymy nowego IncompleteUser
                        IncompleteUser newUser = new IncompleteUser();
                        newUser.setEmail(userEmail);
                        return incompleteUserRepository.save(newUser);
                    });
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

        // Validate that the list of bicycle IDs is not empty
        if (serviceOrderDto.bicycleIds() == null || serviceOrderDto.bicycleIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No bicycles selected for service"));
        }

        // Zbieramy wszystkie rowery (zarówno kompletne jak i niekompletne)
        List<IncompleteBike> userBikes = new ArrayList<>();
        
        // 1. Sprawdź kompletne rowery w repozytorium Bicycle
        List<Bicycle> completeBikes = bicycleRepository.findAllById(serviceOrderDto.bicycleIds());
        for (Bicycle bike : completeBikes) {
            if (bike.getOwner() != null && bike.getOwner().getId().equals(user.getId())) {
                userBikes.add(bike); // Bicycle dziedziczy po IncompleteBike, więc można dodać bezpośrednio
            } else {
                logger.warning("User " + userEmail + " does not have permission for complete bicycle ID: " + bike.getId());
            }
        }
        
        // 2. Sprawdź niekompletne rowery, które nie są kompletne (unikając duplikatów)
        // Zbierz ID już znalezionych kompletnych rowerów
        List<Long> foundBikeIds = completeBikes.stream().map(IncompleteBike::getId).collect(Collectors.toList());
        
        // Znajdź tylko te niekompletne rowery, których nie ma w kompletnych
        List<Long> remainingIds = serviceOrderDto.bicycleIds().stream()
                .filter(id -> !foundBikeIds.contains(id))
                .collect(Collectors.toList());
        
        if (!remainingIds.isEmpty()) {
            List<IncompleteBike> incompleteBikes = incompleteBikeRepository.findAllById(remainingIds);
            for (IncompleteBike bike : incompleteBikes) {
                if (bike.getOwner() != null && bike.getOwner().getId().equals(user.getId())) {
                    userBikes.add(bike);
                } else {
                    logger.warning("User " + userEmail + " does not have permission for incomplete bicycle ID: " + bike.getId());
                }
            }
        }
        
        if (userBikes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No valid bicycles found or you don't have permission for any of the selected bicycles"));
        }
        
        // Utwórz wszystkie obiekty ServiceOrder
        ServicePackage finalServicePackage = servicePackage;
        List<ServiceOrder> serviceOrders = userBikes.stream()
                .map(bike -> {
                    ServiceOrder serviceOrder = new ServiceOrder();
                    serviceOrder.setBicycle(bike);
                    serviceOrder.setClient(user);
                    serviceOrder.setServicePackage(finalServicePackage);
                    serviceOrder.setServicePackageCode(finalServicePackage.getCode()); // Store the code for backward compatibility
                    serviceOrder.setPickupDate(serviceOrderDto.pickupDate());
                    serviceOrder.setPickupAddress(serviceOrderDto.pickupAddress());
                    serviceOrder.setPickupLatitude(serviceOrderDto.pickupLatitude());
                    serviceOrder.setPickupLongitude(serviceOrderDto.pickupLongitude());
                    serviceOrder.setPrice(finalServicePackage.getPrice());
                    serviceOrder.setAdditionalNotes(serviceOrderDto.additionalNotes());
                    serviceOrder.setStatus(ServiceOrder.OrderStatus.PENDING);
                    return serviceOrder;
                })
                .collect(Collectors.toList());
        
        // Zapisz wszystkie ServiceOrder jedną operacją
        List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(serviceOrders);
        
        // Pobierz identyfikatory zapisanych zamówień
        List<Long> createdOrderIds = savedOrders.stream()
                .map(ServiceOrder::getId)
                .collect(Collectors.toList());

        // Sprawdź czy były rowery, których nie udało się znaleźć w bazie danych
        int foundBikes = completeBikes.size() + (remainingIds.isEmpty() ? 0 : incompleteBikeRepository.findAllById(remainingIds).size());
        if (foundBikes < serviceOrderDto.bicycleIds().size()) {
            logger.warning("Some bicycle IDs were not found in the database: " + 
                (serviceOrderDto.bicycleIds().size() - foundBikes) + " bikes missing");
        }

        return ResponseEntity.ok(Map.of(
                "message", "Service orders created successfully",
                "orderIds", createdOrderIds
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
    public ResponseEntity<?> getServicePackagePrice(String servicePackageCode) {
        // Fixed method to accept a String servicePackageCode instead of enum

        Optional<ServicePackage> packageEntity = servicePackageRepository.findByCode(servicePackageCode);
        if (packageEntity.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid service package"));
        }

        return ResponseEntity.ok(Map.of(
                "servicePackage", servicePackageCode,
                "price", packageEntity.get().getPrice()
        ));
    }
}