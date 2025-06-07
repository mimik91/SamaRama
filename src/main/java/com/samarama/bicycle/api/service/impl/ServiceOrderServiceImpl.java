package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import com.samarama.bicycle.api.service.helper.OrderValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private static final Logger logger = Logger.getLogger(ServiceOrderServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;

    public ServiceOrderServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService,
            EmailService emailService) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
    }

    // === CREATION METHODS ===

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrTransportOrderDto dto, String userEmail) {
        try {
            // === NORMALIZACJA DTO ===
            normalizeServiceOrderDto(dto, userEmail);

            // Walidacja danych wejściowych
            if (!dto.isValidForLoggedUser()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia"));
            }

            if (!dto.isServiceOrder()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy jest wymagany"));
            }

            // Walidacja daty
            if (dto.getPickupDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
            }

            // Sprawdź dostępność slotów
            int bikesCount = dto.getBicycleIds().size();
            if (!serviceSlotService.areSlotsAvailable(dto.getPickupDate(), bikesCount)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Brak dostępnych miejsc na wybrany dzień",
                        "availableSlots", getAvailableSlots(dto.getPickupDate())
                ));
            }

            // Pobierz użytkownika
            User user = getUserByEmail(userEmail);

            // Pobierz pakiet serwisowy
            ServicePackage servicePackage = servicePackageRepository.findById(dto.getServicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));

            // Pobierz serwis własny
            BikeService ownService = getOwnService();

            // Walidacja rowerów
            List<IncompleteBike> bikes = validateAndGetBikes(dto.getBicycleIds(), user.getId());

            // Utwórz zamówienia serwisowe
            List<ServiceOrder> orders = createServiceOrdersFromDto(bikes, user, dto, servicePackage, ownService);

            // Zapisz zamówienia
            List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

            // Wyślij powiadomienia email
            sendEmailNotifications(savedOrders);

            // Log akcji
            savedOrders.forEach(order -> logServiceOrderCreation(order, userEmail));

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                    "orderCount", savedOrders.size()
            ));

        } catch (RuntimeException e) {
            logger.warning("Failed to create service order for user " + userEmail + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.severe("Error creating service order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Wystąpił błąd podczas tworzenia zamówienia"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> createGuestServiceOrder(ServiceOrTransportOrderDto dto) {
        try {

            if (!dto.isValidForGuest()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia gościa"));
            }

            if (!dto.isServiceOrder()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy jest wymagany"));
            }

            // Walidacja daty
            if (dto.getPickupDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
            }

            // Sprawdź dostępność slotów
            int bikesCount = dto.getBicycles().size();
            if (!serviceSlotService.areSlotsAvailable(dto.getPickupDate(), bikesCount)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Brak dostępnych miejsc na wybrany dzień",
                        "availableSlots", getAvailableSlots(dto.getPickupDate())
                ));
            }

            // Utwórz lub znajdź użytkownika tymczasowego
            IncompleteUser guestUser = createOrFindIncompleteUser(dto.getEmail(), dto.getPhone());

            // Pobierz pakiet serwisowy
            ServicePackage servicePackage = servicePackageRepository.findById(dto.getServicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));

            // Pobierz serwis własny
            BikeService ownService = getOwnService();

            // Utwórz rowery
            List<IncompleteBike> bikes = createIncompleteBikes(dto.getBicycles(), guestUser);

            // Utwórz zamówienia serwisowe
            List<ServiceOrder> orders = createServiceOrdersFromDto(bikes, guestUser, dto, servicePackage, ownService);

            // Zapisz zamówienia
            List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                    "guestUserId", guestUser.getId()
            ));

        } catch (RuntimeException e) {
            logger.warning("Failed to create guest service order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.severe("Error creating guest service order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Wystąpił błąd podczas tworzenia zamówienia"));
        }
    }

    // === RETRIEVAL METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getUserServiceOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<ServiceOrder> orders = serviceOrderRepository.findByClient(user);

        return orders.stream()
                .map(UnifiedOrderResponseDto::fromServiceOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<ServiceOrderDetailsResponseDto> getServiceOrderDetails(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Check ownership
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(ServiceOrderDetailsResponseDto.fromServiceOrder(order));
    }

    // === UPDATE METHODS ===

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrTransportOrderDto dto, String userEmail) {
        try {
            User user = getUserByEmail(userEmail);
            ServiceOrder order = getServiceOrderForUser(orderId, user);

            // Check if can modify
            if (!canUserModifyOrder(order, userEmail)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
                ));
            }

            // Update fields
            updateServiceOrderFields(order, dto);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienie zostało zaktualizowane",
                    "order", UnifiedOrderResponseDto.fromServiceOrder(order)
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }



    @Override
    @Transactional
    public ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            order.setServiceNotes(notes);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Notatki serwisowe zostały zaktualizowane",
                    "serviceNotes", notes
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // === ADMIN METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getAllServiceOrders() {
        List<ServiceOrder> orders = serviceOrderRepository.findAllActive();
        return orders.stream()
                .map(UnifiedOrderResponseDto::fromServiceOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrderByAdmin(Long orderId, ServiceOrTransportOrderDto dto, String adminEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            updateServiceOrderFields(order, dto);
            order.setLastModifiedBy(adminEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienie zostało zaktualizowane",
                    "order", UnifiedOrderResponseDto.fromServiceOrder(order)
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteServiceOrder(Long orderId, String adminEmail) {
        if (!serviceOrderRepository.existsById(orderId)) {
            return ResponseEntity.notFound().build();
        }

        serviceOrderRepository.deleteById(orderId);
        logger.info("Admin " + adminEmail + " deleted service order " + orderId);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało usunięte"));
    }

    // === UNIFIED ORDER METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getAllServiceOrdersAsUnified() {
        return getAllServiceOrders();
    }

    @Override
    public Optional<UnifiedOrderResponseDto> getOrderAsUnified(Long orderId) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        return orderOpt.map(UnifiedOrderResponseDto::fromServiceOrder);
    }

    // === NORMALIZATION METHODS ===

    /**
     * Normalizuje DTO dla zamówienia serwisowego użytkownika zalogowanego
     * Ustawia userId, targetServiceId oraz transportPrice na odpowiednie wartości
     */
    private void normalizeServiceOrderDto(ServiceOrTransportOrderDto dto, String userEmail) {
        // 1. Przypisz userId zalogowanego użytkownika
        if (dto.getUserId() == null) {
            Optional<User> user = userRepository.findByEmail(userEmail);
            if(user.isPresent()){
                dto.setUserId(user.get().getId());
                logger.info("Assigned userId " + user.get().getId() + " to service order for user: " + userEmail);
            }
        }

        // 2. Ustaw targetServiceId na serwis własny (zamówienia serwisowe zawsze do nas)
        if (dto.getTargetServiceId() == null) {
            dto.setTargetServiceId(Long.parseLong(OrderValidator.internalServiceIdString));
            logger.info("Set targetServiceId to " + OrderValidator.internalServiceIdString + " (internal service) for service order");
        }

        // 3. Ustaw transportPrice na 0 (dla serwisu transport wliczony w cenę pakietu)
        if (dto.getTransportPrice() == null) {
            dto.setTransportPrice(BigDecimal.ZERO);
            logger.info("Set transportPrice to 0 for service order (included in package price)");
        }
    }

    // === PRIVATE HELPER METHODS ===

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private ServiceOrder getServiceOrderById(Long orderId) {
        return serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Service order not found: " + orderId));
    }

    private ServiceOrder getServiceOrderForUser(Long orderId, User user) {
        ServiceOrder order = getServiceOrderById(orderId);

        if (!order.getClient().getId().equals(user.getId())) {
            throw new RuntimeException("Brak uprawnień do zamówienia: " + orderId);
        }

        return order;
    }

    private BikeService getOwnService() {
        return bikeServiceRepository.findById(Long.parseLong(OrderValidator.internalServiceIdString))
                .orElseThrow(() -> new RuntimeException("Internal service not found: " + OrderValidator.internalServiceIdString));
    }

    private List<IncompleteBike> validateAndGetBikes(List<Long> bicycleIds, Long userId) {
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

    private IncompleteUser createOrFindIncompleteUser(String email, String phone) {
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

    private List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IncompleteUser owner) {
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

    private List<ServiceOrder> createServiceOrdersFromDto(
            List<IncompleteBike> bikes, IncompleteUser guestUser, ServiceOrTransportOrderDto dto,
            ServicePackage servicePackage, BikeService ownService) {

        List<ServiceOrder> orders = new ArrayList<>();
        BigDecimal transportPrice = dto.getTransportPrice(); // Używamy znormalizowanej ceny (0)

        for (IncompleteBike bike : bikes) {
            ServiceOrder order = new ServiceOrder();

            // Pola bazowe z TransportOrder
            order.setBicycle(bike);
            order.setClient(guestUser);
            order.setPickupDate(dto.getPickupDate());

            // Nowa struktura adresu odbioru - rozbita na pola
            order.setPickupStreet(dto.getPickupStreet());
            order.setPickupBuilding(dto.getPickupBuildingNumber());
            order.setPickupApartment(dto.getPickupApartmentNumber());
            order.setPickupCity(dto.getPickupCity());
            order.setPickupPostalCode(dto.getPickupPostalCode());
            order.setPickupLatitude(dto.getPickupLatitude());
            order.setPickupLongitude(dto.getPickupLongitude());

            order.setPickupTimeFrom(null);
            order.setPickupTimeTo(null);

            order.setTargetService(ownService);

            // Nowa struktura adresu dostawy - rozbita na pola
            order.setDeliveryStreet(ownService.getStreet());
            order.setDeliveryBuilding(ownService.getBuilding());
            order.setDeliveryApartment(ownService.getFlat());
            order.setDeliveryCity(ownService.getCity());
            order.setDeliveryPostalCode(ownService.getPostalCode());
            order.setDeliveryLatitude(ownService.getLatitude());
            order.setDeliveryLongitude(ownService.getLongitude());

            order.setTransportPrice(transportPrice);
            order.setEstimatedTime(60);
            order.setTransportNotes(dto.getTransportNotes());
            order.setAdditionalNotes(dto.getAdditionalNotes());
            order.setStatus(TransportOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            // Pola specyficzne dla ServiceOrder
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(servicePackage.getPrice());
            order.setServiceNotes(dto.getServiceNotes());
            order.setServiceStartDate(null);
            order.setServiceCompletionDate(null);

            orders.add(order);
        }

        return orders;
    }

    private void updateServiceOrderFields(ServiceOrder order, ServiceOrTransportOrderDto dto) {
        if (dto.getPickupDate() != null) {
            order.setPickupDate(dto.getPickupDate());
        }
        if (dto.getTransportPrice() != null) {
            order.setTransportPrice(dto.getTransportPrice());
        }
        if (dto.getTransportNotes() != null) {
            order.setTransportNotes(dto.getTransportNotes());
        }
        if (dto.getAdditionalNotes() != null) {
            order.setAdditionalNotes(dto.getAdditionalNotes());
        }
        if (dto.getServiceNotes() != null) {
            order.setServiceNotes(dto.getServiceNotes());
        }
        if (dto.getServicePackageId() != null) {
            ServicePackage servicePackage = servicePackageRepository.findById(dto.getServicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(servicePackage.getPrice());
        }
    }

    private boolean canUserModifyOrder(ServiceOrder order, String userEmail) {
        return order.canBeCancelled();
    }

    private int getAvailableSlots(LocalDate date) {
        int maxSlots = serviceSlotService.getMaxBikesPerDay(date);
        int usedSlots = serviceOrderRepository.countByPickupDate(date);
        return Math.max(0, maxSlots - usedSlots);
    }

    private void sendEmailNotifications(List<ServiceOrder> orders) {
        for (ServiceOrder order : orders) {
            try {
                emailService.sendOrderNotificationEmail(order);
            } catch (Exception e) {
                logger.warning("Failed to send email notification for service order: " + order.getId());
            }
        }
    }

    private void logServiceOrderCreation(ServiceOrder order, String userEmail) {
        logger.info(String.format(
                "Service order created: ID=%d, User=%s, Package=%s, Date=%s, TotalPrice=%s",
                order.getId(),
                userEmail,
                order.getServicePackageCode(),
                order.getPickupDate(),
                order.getTotalPrice()
        ));
    }
}