package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Helper class for ServiceOrder operations
 * Handles validation, creation, and updates of ServiceOrder entities
 */
@Component
public class ServiceOrderHelper {

    private static final Logger logger = Logger.getLogger(ServiceOrderHelper.class.getName());

    private final UserRepository userRepository;
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BikeServiceRepository bikeServiceRepository;

    public ServiceOrderHelper(
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository) {
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
    }

    // === USER MANAGEMENT ===

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public IncompleteUser createOrFindIncompleteUser(String email, String phone) {
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            IncompleteUser user = existingUser.get();
            if (phone != null) {
                user.setPhoneNumber(phone);
            }
            return incompleteUserRepository.save(user);
        } else {
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(email);
            newUser.setPhoneNumber(phone);
            newUser.setCreatedAt(LocalDateTime.now());
            return incompleteUserRepository.save(newUser);
        }
    }

    // === SERVICE PACKAGE MANAGEMENT ===

    public ServicePackage getServicePackage(ServiceOrTransportOrderDto dto) {
        if (dto.servicePackageId() != null) {
            return servicePackageRepository.findById(dto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else if (dto.servicePackageCode() != null) {
            return servicePackageRepository.findByCode(dto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else {
            throw new RuntimeException("Pakiet serwisowy jest wymagany");
        }
    }

    public BikeService getOwnService() {
        return bikeServiceRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Own service not found"));
    }

    // === BICYCLE MANAGEMENT ===

    public List<IncompleteBike> validateAndGetBikes(List<Long> bicycleIds, Long userId) {
        List<IncompleteBike> bikes = new ArrayList<>();
        for (Long bicycleId : bicycleIds) {
            IncompleteBike bike = incompleteBikeRepository.findById(bicycleId)
                    .orElseThrow(() -> new RuntimeException("Bike not found: " + bicycleId));

            if (bike.getOwner() == null || !bike.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Brak uprawnień do roweru: " + bicycleId);
            }

            bikes.add(bike);
        }
        return bikes;
    }

    public List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IncompleteUser owner) {
        List<IncompleteBike> bikes = new ArrayList<>();

        for (GuestBicycleDto bikeDto : bicycleDtos) {
            IncompleteBike bike = new IncompleteBike();
            bike.setBrand(bikeDto.brand());
            bike.setModel(bikeDto.model());
            if (bikeDto.additionalInfo() != null && !bikeDto.additionalInfo().isEmpty()) {
                bike.setType(bikeDto.additionalInfo());
            }
            bike.setOwner(owner);
            bike.setCreatedAt(LocalDateTime.now());

            bikes.add(incompleteBikeRepository.save(bike));
        }

        return bikes;
    }

    // === SERVICE ORDER CREATION ===

    public List<ServiceOrder> createServiceOrdersForUser(List<IncompleteBike> bikes, User user,
                                                         ServiceOrTransportOrderDto dto, ServicePackage servicePackage,
                                                         BikeService ownService) {
        List<ServiceOrder> orders = new ArrayList<>();

        for (IncompleteBike bike : bikes) {
            ServiceOrder order = createServiceOrderBase(bike, user, dto, servicePackage, ownService);
            orders.add(order);
        }

        return orders;
    }

    public List<ServiceOrder> createServiceOrdersForGuest(List<IncompleteBike> bikes, IncompleteUser guestUser,
                                                          ServiceOrTransportOrderDto dto, ServicePackage servicePackage,
                                                          BikeService ownService) {
        List<ServiceOrder> orders = new ArrayList<>();

        for (IncompleteBike bike : bikes) {
            ServiceOrder order = createServiceOrderBase(bike, guestUser, dto, servicePackage, ownService);
            // For guest orders, append city to address
            if (dto.city() != null) {
                order.setPickupAddress(dto.pickupAddress() + ", " + dto.city());
            }
            orders.add(order);
        }

        return orders;
    }

    private ServiceOrder createServiceOrderBase(IncompleteBike bike, IncompleteUser client,
                                                ServiceOrTransportOrderDto dto, ServicePackage servicePackage,
                                                BikeService ownService) {
        ServiceOrder order = new ServiceOrder();

        // Set transport fields (base TransportOrder)
        order.setBicycle(bike);
        order.setClient(client);
        order.setPickupDate(dto.pickupDate());
        order.setPickupAddress(dto.pickupAddress());
        order.setPickupLatitude(dto.pickupLatitude());
        order.setPickupLongitude(dto.pickupLongitude());
        order.setPickupTimeFrom(dto.pickupTimeFrom());
        order.setPickupTimeTo(dto.pickupTimeTo());

        order.setTargetService(ownService);
        order.setDeliveryAddress("SERWIS WŁASNY");
        order.setDeliveryLatitude(ownService.getLatitude());
        order.setDeliveryLongitude(ownService.getLongitude());

        order.setTransportPrice(dto.transportPrice() != null ? dto.transportPrice() : BigDecimal.ZERO);
        order.setEstimatedTime(dto.estimatedTime());
        order.setTransportNotes(dto.transportNotes());
        order.setAdditionalNotes(dto.additionalNotes());
        order.setStatus(TransportOrder.OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        // Set service fields (ServiceOrder specific)
        order.setServicePackage(servicePackage);
        order.setServicePackageCode(servicePackage.getCode());
        order.setServicePrice(dto.servicePrice() != null ? dto.servicePrice() : servicePackage.getPrice());
        order.setServiceNotes(dto.serviceNotes());

        return order;
    }

    // === SERVICE ORDER UPDATES ===

    public void updateServiceOrderFields(ServiceOrder order, ServiceOrTransportOrderDto dto) {
        // Update transport fields
        updateTransportFields(order, dto);

        // Update service-specific fields
        updateServiceFields(order, dto);

        // Update bicycle if provided and owned by client
        updateBicycleIfProvided(order, dto);
    }

    private void updateTransportFields(ServiceOrder order, ServiceOrTransportOrderDto dto) {
        if (dto.pickupDate() != null) {
            order.setPickupDate(dto.pickupDate());
        }
        if (dto.pickupAddress() != null) {
            order.setPickupAddress(dto.pickupAddress());
        }
        if (dto.pickupLatitude() != null) {
            order.setPickupLatitude(dto.pickupLatitude());
        }
        if (dto.pickupLongitude() != null) {
            order.setPickupLongitude(dto.pickupLongitude());
        }
        if (dto.pickupTimeFrom() != null) {
            order.setPickupTimeFrom(dto.pickupTimeFrom());
        }
        if (dto.pickupTimeTo() != null) {
            order.setPickupTimeTo(dto.pickupTimeTo());
        }
        if (dto.transportPrice() != null) {
            order.setTransportPrice(dto.transportPrice());
        }
        if (dto.estimatedTime() != null) {
            order.setEstimatedTime(dto.estimatedTime());
        }
        if (dto.transportNotes() != null) {
            order.setTransportNotes(dto.transportNotes());
        }
        if (dto.additionalNotes() != null) {
            order.setAdditionalNotes(dto.additionalNotes());
        }
    }

    private void updateServiceFields(ServiceOrder order, ServiceOrTransportOrderDto dto) {
        if (dto.servicePackageId() != null) {
            ServicePackage servicePackage = servicePackageRepository.findById(dto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(servicePackage.getPrice());
        } else if (dto.servicePackageCode() != null) {
            ServicePackage servicePackage = servicePackageRepository.findByCode(dto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(servicePackage.getPrice());
        }

        if (dto.servicePrice() != null) {
            order.setServicePrice(dto.servicePrice());
        }

        if (dto.serviceNotes() != null) {
            order.setServiceNotes(dto.serviceNotes());
        }
    }

    private void updateBicycleIfProvided(ServiceOrder order, ServiceOrTransportOrderDto dto) {
        if (dto.bicycleIds() != null && !dto.bicycleIds().isEmpty()) {
            Long bicycleId = dto.bicycleIds().get(0); // Take first bicycle
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();
                if (bike.getOwner().getId().equals(order.getClient().getId())) {
                    order.setBicycle(bike);
                }
            }
        }
    }

    // === VALIDATION METHODS ===

    public void validateServiceOrderData(ServiceOrTransportOrderDto dto, boolean isGuest) {
        if (isGuest) {
            validateGuestOrderData(dto);
        } else {
            validateUserOrderData(dto);
        }
    }

    private void validateUserOrderData(ServiceOrTransportOrderDto dto) {
        if (!dto.isValidForLoggedUser()) {
            throw new RuntimeException("Nieprawidłowe dane zamówienia");
        }
    }

    private void validateGuestOrderData(ServiceOrTransportOrderDto dto) {
        if (!dto.isValidForGuest()) {
            throw new RuntimeException("Nieprawidłowe dane zamówienia gościa");
        }
    }

    public void validatePickupDate(ServiceOrTransportOrderDto dto) {
        if (dto.pickupDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Data odbioru nie może być w przeszłości");
        }

        java.time.LocalDate maxDate = java.time.LocalDate.now().plusMonths(1);
        if (dto.pickupDate().isAfter(maxDate)) {
            throw new RuntimeException("Data odbioru nie może być odleglejsza niż miesiąc");
        }
    }

    public void validateCity(String address, String city, CityValidator cityValidator) {
        String extractedCity = cityValidator.extractCityFromAddress(address);
        if (extractedCity == null || !cityValidator.isValidCity(extractedCity)) {
            throw new RuntimeException("Nieprawidłowe miasto");
        }
    }

    // === BUSINESS LOGIC HELPERS ===

    public boolean canUserModifyOrder(ServiceOrder order, String userEmail) {
        // Check ownership
        if (!order.getClient().getEmail().equals(userEmail)) {
            return false;
        }

        // Check status
        return order.canBeCancelled();
    }

    public boolean canStartService(ServiceOrder order) {
        return order.canStartService();
    }

    public boolean canCompleteService(ServiceOrder order) {
        return order.isServiceInProgress();
    }

    // === LOGGING HELPERS ===

    public void logServiceOrderCreation(ServiceOrder order, String userEmail) {
        logger.info(String.format(
                "Service order created: ID=%d, User=%s, Package=%s, Date=%s, Transport=%s, Service=%s",
                order.getId(),
                userEmail,
                order.getServicePackageCode(),
                order.getPickupDate(),
                order.getTransportPrice(),
                order.getServicePrice()
        ));
    }

    public void logServiceAction(String action, Long orderId, String userEmail) {
        logger.info(String.format(
                "Service action: %s, OrderID=%d, User=%s",
                action, orderId, userEmail
        ));
    }
}