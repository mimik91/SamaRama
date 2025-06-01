package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceSlotService;
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
    private final TransportOrderRepository transportOrderRepository;
    private final UserRepository userRepository;
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;
    private final CityValidator cityValidator;

    public ServiceOrderServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            TransportOrderRepository transportOrderRepository,
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService,
            EmailService emailService,
            CityValidator cityValidator) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
        this.cityValidator = cityValidator;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrderDto dto, String userEmail) {
        // Walidacja danych wejściowych
        if (!dto.isValidForLoggedUser()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia"));
        }

        // Walidacja daty
        if (dto.pickupDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        LocalDate maxDate = LocalDate.now().plusMonths(1);
        if (dto.pickupDate().isAfter(maxDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być odleglejsza niż miesiąc"));
        }

        // Walidacja miasta
        String city = cityValidator.extractCityFromAddress(dto.pickupAddress());
        if (city == null || !cityValidator.isValidCity(city)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto"));
        }

        // Walidacja dostępności slotów serwisowych
        int bikesCount = dto.bicycleIds().size();

        if (!serviceSlotService.isWithinMaxBikesPerOrder(dto.pickupDate(), bikesCount)) {
            int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(dto.pickupDate());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Przekroczono maksymalną liczbę rowerów na jedno zamówienie (" + maxPerOrder + ")",
                    "maxBikesPerOrder", maxPerOrder
            ));
        }

        if (!serviceSlotService.areSlotsAvailable(dto.pickupDate(), bikesCount)) {
            int maxPerDay = serviceSlotService.getMaxBikesPerDay(dto.pickupDate());
            int booked = serviceOrderRepository.countByPickupDate(dto.pickupDate());
            int available = Math.max(0, maxPerDay - booked);

            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak wystarczającej liczby wolnych miejsc na wybrany dzień. Dostępne miejsca: " + available,
                    "availableBikes", available,
                    "maxBikesPerDay", maxPerDay
            ));
        }

        // Pobierz użytkownika
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pobierz pakiet serwisowy
        ServicePackage servicePackage = null;
        if (dto.servicePackageId() != null) {
            servicePackage = servicePackageRepository.findById(dto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else if (dto.servicePackageCode() != null) {
            servicePackage = servicePackageRepository.findByCode(dto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy jest wymagany"));
        }

        // Pobierz serwis własny (ID=1)
        BikeService ownService = bikeServiceRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Own service not found"));

        // Walidacja rowerów
        List<IncompleteBike> bikes = validateAndGetBikes(dto.bicycleIds(), user.getId());

        // Utwórz zamówienia serwisowe (jedno na rower)
        List<ServiceOrder> orders = new ArrayList<>();
        for (IncompleteBike bike : bikes) {
            ServiceOrder order = new ServiceOrder();

            // Ustaw pola z TransportOrder (bazowe)
            order.setBicycle(bike);
            order.setClient(user);
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

            // Ustaw pola specyficzne dla ServiceOrder
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(dto.servicePrice() != null ? dto.servicePrice() : servicePackage.getPrice());
            order.setServiceNotes(dto.serviceNotes());

            orders.add(order);
        }

        // Zapisz zamówienia
        List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

        // Wyślij powiadomienia email
        for (ServiceOrder order : savedOrders) {
            try {
                emailService.sendOrderNotificationEmail(order);
            } catch (Exception e) {
                logger.warning("Failed to send email notification for service order: " + order.getId());
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                "orderCount", savedOrders.size()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createGuestServiceOrder(ServiceOrderDto dto) {
        // Walidacja danych gościa
        if (!dto.isValidForGuest()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia gościa"));
        }

        // Walidacja daty
        if (dto.pickupDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        // Walidacja miasta
        if (dto.city() == null || !cityValidator.isValidCity(dto.city())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto"));
        }

        // Walidacja dostępności slotów
        int bikesCount = dto.bicycles().size();
        if (!serviceSlotService.areSlotsAvailable(dto.pickupDate(), bikesCount)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak dostępnych miejsc na wybrany dzień"
            ));
        }

        // Utwórz lub znajdź użytkownika tymczasowego
        IncompleteUser guestUser = createOrFindIncompleteUser(dto.clientEmail(), dto.clientPhone());

        // Pobierz pakiet serwisowy
        ServicePackage servicePackage = null;
        if (dto.servicePackageId() != null) {
            servicePackage = servicePackageRepository.findById(dto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else if (dto.servicePackageCode() != null) {
            servicePackage = servicePackageRepository.findByCode(dto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy jest wymagany"));
        }

        // Pobierz serwis własny
        BikeService ownService = bikeServiceRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Own service not found"));

        // Utwórz rowery
        List<IncompleteBike> bikes = createIncompleteBikes(dto.bicycles(), guestUser);

        // Utwórz zamówienia serwisowe
        List<ServiceOrder> orders = new ArrayList<>();
        for (IncompleteBike bike : bikes) {
            ServiceOrder order = new ServiceOrder();

            // Bazowe pola
            order.setBicycle(bike);
            order.setClient(guestUser);
            order.setPickupDate(dto.pickupDate());
            order.setPickupAddress(dto.pickupAddress() + ", " + dto.city());
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

            // Pola serwisowe
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setServicePrice(dto.servicePrice() != null ? dto.servicePrice() : servicePackage.getPrice());
            order.setServiceNotes(dto.serviceNotes());

            orders.add(order);
        }

        // Zapisz zamówienia
        List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                "guestUserId", guestUser.getId()
        ));
    }

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
    public ResponseEntity<UnifiedOrderResponseDto> getServiceOrderDetails(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(UnifiedOrderResponseDto.fromServiceOrder(order));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrderDto dto, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Sprawdź czy można modyfikować
        if (!order.canBeCancelled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        // Aktualizuj pola
        updateServiceOrderFields(order, dto);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", UnifiedOrderResponseDto.fromServiceOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> startService(Long orderId, String userEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        if (!order.canStartService()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Serwis można rozpocząć tylko w statusie CONFIRMED lub PICKED_UP"
            ));
        }

        order.startService();
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Serwis został rozpoczęty",
                "serviceStartDate", order.getServiceStartDate()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> completeService(Long orderId, String userEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        if (!order.isServiceInProgress()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Serwis można zakończyć tylko w statusie IN_SERVICE"
            ));
        }

        order.completeService();
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Serwis został zakończony",
                "serviceCompletionDate", order.getServiceCompletionDate(),
                "serviceDuration", order.getServiceDurationDisplay()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();
        order.setServiceNotes(notes);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Notatki serwisowe zostały zaktualizowane",
                "serviceNotes", notes
        ));
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
    public ResponseEntity<?> updateServiceOrderByAdmin(Long orderId, ServiceOrderDto dto, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();
        updateServiceOrderFields(order, dto);
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", UnifiedOrderResponseDto.fromServiceOrder(order)
        ));
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

    // === STATISTICS ===

    @Override
    public List<Object[]> getServicePackageStatistics() {
        return serviceOrderRepository.getServicePackageStatistics();
    }

    @Override
    public Double getAverageServiceTime() {
        return serviceOrderRepository.getAverageServiceTimeInMinutes();
    }

    @Override
    public List<Object[]> getServiceRevenue() {
        return serviceOrderRepository.getServicePackageRevenue();
    }

    // === HELPER METHODS ===

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
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
}